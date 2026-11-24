package com.minsoojun.kakaostickerstudio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 100% On-Device KakaoTalk 2D SD Emoticon Generation, Slicing, and Alpha Matting Engine.
 * Runs entirely inside the Android app without any Cloud Run dependency.
 * Communicates directly with Google Gemini API (generativelanguage.googleapis.com) using the user's API Key.
 */
public class OnDeviceStickerEngine {

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

    public static final String[] IMAGE_MODELS_PRIORITY = new String[]{
            "gemini-3-pro-image-preview",
            "gemini-3.1-flash-image-preview",
            "gemini-2.5-flash-image",
            "gemini-2.0-flash-exp-image-generation"
    };

    public static final String[] WHITE_BG_MODELS = new String[]{
            "gemini-2.5-flash-image",
            "gemini-3.1-flash-image-preview",
            "gemini-2.0-flash-exp-image-generation",
            "gemini-3-pro-image-preview"
    };

    public static class SlicedSticker {
        public final int index;
        public final String filename;
        public final String phrase;
        public final byte[] pngBytes;
        public final String dataUrl;
        public final double sizeKb;

        public SlicedSticker(int index, String filename, String phrase, byte[] pngBytes) {
            this.index = index;
            this.filename = filename;
            this.phrase = phrase;
            this.pngBytes = pngBytes;
            this.dataUrl = "data:image/png;base64," + Base64.encodeToString(pngBytes, Base64.NO_WRAP);
            this.sizeKb = Math.round((pngBytes.length / 1024.0) * 10.0) / 10.0;
        }
    }

    public static class SliceResult {
        public final byte[] rawSheetPngBytes;
        public final byte[] displaySheetPngBytes;
        public final String sheetDataUrl;
        public final List<SlicedSticker> stickers;
        public final byte[] zipBytes;
        public final String bgMode;
        public final int stickerSize;
        public final String usedModel;

        public SliceResult(
                byte[] rawSheetPngBytes,
                byte[] displaySheetPngBytes,
                List<SlicedSticker> stickers,
                byte[] zipBytes,
                String bgMode,
                int stickerSize,
                String usedModel) {
            this.rawSheetPngBytes = rawSheetPngBytes;
            this.displaySheetPngBytes = displaySheetPngBytes;
            this.sheetDataUrl = "data:image/png;base64," + Base64.encodeToString(displaySheetPngBytes, Base64.NO_WRAP);
            this.stickers = stickers;
            this.zipBytes = zipBytes;
            this.bgMode = bgMode;
            this.stickerSize = stickerSize;
            this.usedModel = usedModel;
        }
    }

    public static JSONObject verifyApiKey(String apiKey) {
        JSONObject result = new JSONObject();
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                result.put("ok", false);
                result.put("message", "API 키가 비어 있습니다.");
                return result;
            }
            String cleanKey = apiKey.trim();
            URL url = new URL(GEMINI_API_BASE + "?key=" + cleanKey + "&pageSize=100");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            String body = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code >= 200 && code < 300) {
                JSONObject json = new JSONObject(body);
                JSONArray models = json.optJSONArray("models");
                List<String> foundImageModels = new ArrayList<>();
                if (models != null) {
                    for (int i = 0; i < models.length(); i++) {
                        JSONObject m = models.optJSONObject(i);
                        if (m != null) {
                            String name = m.optString("name", "").replace("models/", "");
                            if (name.contains("image") || name.contains("gemini-2.0-flash-exp") || name.contains("gemini-3")) {
                                foundImageModels.add(name);
                            }
                        }
                    }
                }
                result.put("ok", true);
                result.put("message", "✅ Gemini API 키 인증 성공! (사용 가능 이미지 모델 " + foundImageModels.size() + "개 확인)");
                result.put("image_models", new JSONArray(foundImageModels));
            } else {
                String errDetail = extractErrorMessage(body);
                result.put("ok", false);
                result.put("message", "❌ API 키 확인 실패 (HTTP " + code + "): " + errDetail);
            }
        } catch (Exception e) {
            try {
                result.put("ok", false);
                result.put("message", "❌ 네트워크/연결 오류: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static List<String> translatePhrases(String apiKey, String targetLangLabel, List<String> sourcePhrases) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key required for AI translation");
        }
        String prompt = "You are an expert KakaoTalk / LINE messenger sticker copywriter.\n"
                + "Translate the following 15 short messenger sticker phrases into natural, punchy, short "
                + targetLangLabel
                + " (each phrase MUST be very short, 1 to 4 words max, suitable for bold sticker text above a character).\n"
                + "Return ONLY a JSON array of 15 strings in the exact same order.\n\n"
                + "Input phrases: " + new JSONArray(sourcePhrases).toString();

        String[] textModels = new String[]{"gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash"};
        Exception lastErr = null;
        for (String model : textModels) {
            try {
                JSONObject payload = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject userContent = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", prompt));
                userContent.put("role", "user");
                userContent.put("parts", parts);
                contents.put(userContent);
                payload.put("contents", contents);

                String respStr = postJson(GEMINI_API_BASE + "/" + model + ":generateContent?key=" + apiKey.trim(), payload.toString(), 30000);
                JSONObject respJson = new JSONObject(respStr);
                JSONArray candidates = respJson.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                    if (content != null) {
                        JSONArray respParts = content.optJSONArray("parts");
                        if (respParts != null) {
                            for (int i = 0; i < respParts.length(); i++) {
                                String txt = respParts.getJSONObject(i).optString("text", "").trim();
                                int start = txt.indexOf('[');
                                int end = txt.lastIndexOf(']');
                                if (start >= 0 && end > start) {
                                    JSONArray arr = new JSONArray(txt.substring(start, end + 1));
                                    if (arr.length() == 15) {
                                        List<String> out = new ArrayList<>(15);
                                        for (int k = 0; k < 15; k++) {
                                            out.add(arr.optString(k, "").trim());
                                        }
                                        return out;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                lastErr = e;
            }
        }
        if (lastErr != null) throw lastErr;
        throw new RuntimeException("번역 결과를 파싱할 수 없습니다.");
    }

    /**
     * Step 1: Center-crop and normalize uploaded photo on-device.
     */
    public static Bitmap cropPersonCenter(byte[] imageBytes, int maxDim) {
        Bitmap src = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (src == null) {
            throw new IllegalArgumentException("이미지 파일을 디코딩할 수 없습니다.");
        }
        int w = src.getWidth();
        int h = src.getHeight();
        float targetRatio = 0.88f;
        float curRatio = (float) w / (float) h;

        Bitmap cropped = src;
        if (curRatio > 1.15f) {
            int newW = Math.min(w, Math.round(h * 0.95f));
            int x0 = Math.max(0, (w - newW) / 2);
            cropped = Bitmap.createBitmap(src, x0, 0, newW, h);
        } else if (curRatio < 0.65f) {
            int newH = Math.min(h, Math.round(w / targetRatio));
            int y0 = Math.max(0, Math.round((h - newH) * 0.35f));
            if (y0 + newH > h) newH = h - y0;
            cropped = Bitmap.createBitmap(src, 0, y0, w, newH);
        }

        int cw = cropped.getWidth();
        int ch = cropped.getHeight();
        if (Math.max(cw, ch) > maxDim) {
            float scale = (float) maxDim / (float) Math.max(cw, ch);
            int nw = Math.max(1, Math.round(cw * scale));
            int nh = Math.max(1, Math.round(ch * scale));
            cropped = Bitmap.createScaledBitmap(cropped, nw, nh, true);
        }
        return cropped;
    }

    /**
     * Step 2: Replace background around the person with pure white (#FFFFFF) using Gemini Image API.
     */
    public static Bitmap replaceBackgroundWithWhite(String apiKey, Bitmap step1Img) {
        byte[] jpegBytes = bitmapToJpegBytes(step1Img, 92);
        String b64Input = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
        String prompt = "Keep the person in the center exactly as they are (preserving facial features, hair, glasses, expression, and clothing), "
                + "and replace the entire background around the person with a clean, solid pure white (#FFFFFF) background. "
                + "Do not add any text, borders, or extra objects.";

        for (String model : WHITE_BG_MODELS) {
            try {
                Bitmap out = callGeminiImageModel(apiKey, model, b64Input, "image/jpeg", prompt, null);
                if (out != null) {
                    return out;
                }
            } catch (Exception ignored) {
            }
        }
        // Fallback to cropped original if Step 2 model is unavailable
        return step1Img;
    }

    /**
     * Step 3: Generate the 3x5 KakaoTalk 2D SD Emoticon Sheet via Gemini Image API.
     */
    public static PairResult generateStickerSheet(String apiKey, Bitmap step2Img, String prompt, String preferredModel) throws Exception {
        byte[] pngBytes = bitmapToPngBytes(step2Img);
        String b64Input = Base64.encodeToString(pngBytes, Base64.NO_WRAP);

        List<String> modelsToTry = new ArrayList<>();
        if (preferredModel != null && !preferredModel.trim().isEmpty()) {
            modelsToTry.add(preferredModel.trim());
        }
        for (String m : IMAGE_MODELS_PRIORITY) {
            if (!modelsToTry.contains(m)) {
                modelsToTry.add(m);
            }
        }

        Exception lastErr = null;
        for (String model : modelsToTry) {
            // First try with aspectRatio "3:4", then fallback without imageConfig if rejected by model
            try {
                Bitmap sheet = callGeminiImageModel(apiKey, model, b64Input, "image/png", prompt, "3:4");
                if (sheet != null) {
                    return new PairResult(sheet, model);
                }
            } catch (Exception e1) {
                lastErr = e1;
                try {
                    Bitmap sheet2 = callGeminiImageModel(apiKey, model, b64Input, "image/png", prompt, null);
                    if (sheet2 != null) {
                        return new PairResult(sheet2, model);
                    }
                } catch (Exception e2) {
                    lastErr = e2;
                }
            }
        }
        String errMsg = lastErr != null ? lastErr.getMessage() : "이미지 생성 응답이 비어 있습니다.";
        throw new RuntimeException("Gemini 이모티콘 시트 생성 실패: " + errMsg);
    }

    public static class PairResult {
        public final Bitmap bitmap;
        public final String model;

        public PairResult(Bitmap bitmap, String model) {
            this.bitmap = bitmap;
            this.model = model;
        }
    }

    private static Bitmap callGeminiImageModel(
            String apiKey,
            String model,
            String base64Image,
            String mimeType,
            String prompt,
            String aspectRatio) throws Exception {

        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject userContent = new JSONObject();
        userContent.put("role", "user");

        JSONArray parts = new JSONArray();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Image);
        parts.put(new JSONObject().put("inlineData", inlineData));
        parts.put(new JSONObject().put("text", prompt));
        userContent.put("parts", parts);
        contents.put(userContent);
        payload.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        JSONArray modalities = new JSONArray();
        modalities.put("TEXT");
        modalities.put("IMAGE");
        genConfig.put("responseModalities", modalities);
        if (aspectRatio != null && !aspectRatio.isEmpty()) {
            JSONObject imgCfg = new JSONObject();
            imgCfg.put("aspectRatio", aspectRatio);
            genConfig.put("imageConfig", imgCfg);
        }
        payload.put("generationConfig", genConfig);

        String endpoint = GEMINI_API_BASE + "/" + model + ":generateContent?key=" + apiKey.trim();
        String respStr = postJson(endpoint, payload.toString(), 180000);

        JSONObject respJson = new JSONObject(respStr);
        JSONArray candidates = respJson.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return null;
        }
        Bitmap lastFoundBmp = null;
        for (int c = 0; c < candidates.length(); c++) {
            JSONObject cand = candidates.optJSONObject(c);
            if (cand == null) continue;
            JSONObject content = cand.optJSONObject("content");
            if (content == null) continue;
            JSONArray rParts = content.optJSONArray("parts");
            if (rParts == null) continue;
            for (int p = 0; p < rParts.length(); p++) {
                JSONObject part = rParts.optJSONObject(p);
                if (part == null || part.optBoolean("thought", false)) continue;
                JSONObject inl = part.optJSONObject("inlineData");
                if (inl == null) {
                    inl = part.optJSONObject("inline_data");
                }
                if (inl != null) {
                    String b64 = inl.optString("data", "");
                    if (!b64.isEmpty()) {
                        byte[] raw = Base64.decode(b64, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                        if (bmp != null) {
                            lastFoundBmp = bmp;
                        }
                    }
                }
            }
        }
        return lastFoundBmp;
    }

    /**
     * Step 4: Corridor-based 3x5 Slicing + White (#FFFFFF) vs Transparent (Alpha=0) Matting + ZIP Packaging.
     */
    public static SliceResult sliceStickerSheet(
            Bitmap sheetRgb,
            List<String> phrases,
            String bgMode,
            int stickerSize,
            String usedModel) throws Exception {

        Bitmap cleaned = cleanOuterBorders(sheetRgb);
        int w = cleaned.getWidth();
        int h = cleaned.getHeight();
        int[] pixels = new int[w * h];
        cleaned.getPixels(pixels, 0, w, 0, 0, w, h);

        // Compute horizontal & vertical non-white density profiles
        double[] colNonWhite = new double[w];
        double[] rowNonWhite = new double[h];
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            int rowCount = 0;
            for (int x = 0; x < w; x++) {
                int c = pixels[rowOffset + x];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r < 240 || g < 240 || b < 240) {
                    rowCount++;
                    colNonWhite[x] += 1.0;
                }
            }
            rowNonWhite[y] = (double) rowCount / (double) w;
        }
        for (int x = 0; x < w; x++) {
            colNonWhite[x] /= (double) h;
        }

        int[] xSplits = findSplitCorridors(colNonWhite, 3, w);
        int[] ySplits = findSplitCorridors(rowNonWhite, 5, h);

        boolean transparent = "transparent".equalsIgnoreCase(bgMode);
        List<SlicedSticker> stickers = new ArrayList<>(15);

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                int x0 = xSplits[c];
                int x1 = Math.max(x0 + 1, xSplits[c + 1]);
                int y0 = ySplits[r];
                int y1 = Math.max(y0 + 1, ySplits[r + 1]);

                Bitmap cell = Bitmap.createBitmap(cleaned, x0, y0, x1 - x0, y1 - y0);
                Bitmap squareSticker = fitCellToSquare(cell, stickerSize, transparent);

                String phrase = (phrases != null && idx < phrases.size()) ? phrases.get(idx) : ("sticker_" + (idx + 1));
                String safePhrase = sanitizeFilename(phrase);
                String fname = String.format("%02d_%s.png", idx + 1, safePhrase);
                byte[] sBytes = bitmapToPngBytes(squareSticker);
                stickers.add(new SlicedSticker(idx + 1, fname, phrase, sBytes));
            }
        }

        Bitmap processedSheet = transparent ? removeOuterWhiteToTransparent(cleaned, 240) : cleaned;
        Bitmap dispSheet = processedSheet;
        if (stickerSize <= 120) {
            int dw = Math.max(1, Math.round(processedSheet.getWidth() * 0.38f));
            int dh = Math.max(1, Math.round(processedSheet.getHeight() * 0.38f));
            dispSheet = Bitmap.createScaledBitmap(processedSheet, dw, dh, true);
        }

        byte[] rawSheetBytes = bitmapToPngBytes(sheetRgb);
        byte[] dispSheetBytes = bitmapToPngBytes(dispSheet);
        byte[] zipBytes = buildZipBundle(dispSheetBytes, stickers);

        return new SliceResult(
                rawSheetBytes,
                dispSheetBytes,
                stickers,
                zipBytes,
                transparent ? "transparent" : "white",
                stickerSize,
                usedModel != null ? usedModel : "gemini-3-pro-image-preview"
        );
    }

    private static Bitmap cleanOuterBorders(Bitmap src) {
        Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
        int w = out.getWidth();
        int h = out.getHeight();
        Canvas canvas = new Canvas(out);
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL);
        int padX = Math.max(4, w / 100);
        int padY = Math.max(4, h / 100);
        canvas.drawRect(0, 0, w, padY, whitePaint);
        canvas.drawRect(0, h - padY, w, h, whitePaint);
        canvas.drawRect(0, 0, padX, h, whitePaint);
        canvas.drawRect(w - padX, 0, w, h, whitePaint);
        return out;
    }

    private static int[] findSplitCorridors(double[] profile, int numCells, int totalLen) {
        int[] splits = new int[numCells + 1];
        splits[0] = 0;
        splits[numCells] = totalLen;
        double step = (double) totalLen / (double) numCells;
        int win = Math.max(8, (int) (step * 0.18));

        for (int i = 1; i < numCells; i++) {
            int expected = (int) Math.round(i * step);
            int lo = Math.max(1, expected - win);
            int hi = Math.min(totalLen - 2, expected + win);
            int bestPos = expected;
            double bestScore = Double.MAX_VALUE;
            for (int pos = lo; pos <= hi; pos++) {
                // Smooth window average of 5 pixels
                double sum = 0;
                int cnt = 0;
                for (int d = -2; d <= 2; d++) {
                    int p = pos + d;
                    if (p >= 0 && p < totalLen) {
                        sum += profile[p];
                        cnt++;
                    }
                }
                double avg = sum / Math.max(1, cnt);
                if (avg < bestScore) {
                    bestScore = avg;
                    bestPos = pos;
                }
            }
            splits[i] = bestPos;
        }
        return splits;
    }

    private static Bitmap fitCellToSquare(Bitmap cellRgb, int targetSize, boolean transparent) {
        int w = cellRgb.getWidth();
        int h = cellRgb.getHeight();
        int[] pixels = new int[w * h];
        cellRgb.getPixels(pixels, 0, w, 0, 0, w, h);

        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            int rowOff = y * w;
            for (int x = 0; x < w; x++) {
                int c = pixels[rowOff + x];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r < 242 || g < 242 || b < 242) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        Bitmap content;
        if (maxX >= minX && maxY >= minY) {
            int pad = 4;
            int x0 = Math.max(0, minX - pad);
            int y0 = Math.max(0, minY - pad);
            int x1 = Math.min(w, maxX + 1 + pad);
            int y1 = Math.min(h, maxY + 1 + pad);
            content = Bitmap.createBitmap(cellRgb, x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
        } else {
            content = cellRgb;
        }

        if (transparent) {
            content = removeOuterWhiteToTransparent(content, 240);
        }

        int cw = content.getWidth();
        int ch = content.getHeight();
        int usable = Math.max(16, Math.round(targetSize * 0.88f));
        float scale = Math.min((float) usable / (float) cw, (float) usable / (float) ch);
        int nw = Math.max(1, Math.round(cw * scale));
        int nh = Math.max(1, Math.round(ch * scale));

        Bitmap resized = Bitmap.createScaledBitmap(content, nw, nh, true);
        Bitmap canvasBmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBmp);
        if (!transparent) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvasBmp.eraseColor(Color.TRANSPARENT);
        }
        int left = (targetSize - nw) / 2;
        int top = (targetSize - nh) / 2;
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(resized, left, top, p);
        return canvasBmp;
    }

    /**
     * Border-connected BFS Flood-Fill that removes only outer white background pixels to transparent (alpha=0),
     * preserving white inside the character (eyes, teeth, white hoodies, pillows, etc.).
     */
    public static Bitmap removeOuterWhiteToTransparent(Bitmap src, int threshold) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        boolean[] isWhite = new boolean[w * h];
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            isWhite[i] = (r >= threshold && g >= threshold && b >= threshold);
        }

        boolean[] visited = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>(w * 2 + h * 2);

        for (int x = 0; x < w; x++) {
            int topIdx = x;
            if (isWhite[topIdx] && !visited[topIdx]) {
                visited[topIdx] = true;
                queue.add(topIdx);
            }
            int botIdx = (h - 1) * w + x;
            if (isWhite[botIdx] && !visited[botIdx]) {
                visited[botIdx] = true;
                queue.add(botIdx);
            }
        }
        for (int y = 0; y < h; y++) {
            int leftIdx = y * w;
            if (isWhite[leftIdx] && !visited[leftIdx]) {
                visited[leftIdx] = true;
                queue.add(leftIdx);
            }
            int rightIdx = y * w + (w - 1);
            if (isWhite[rightIdx] && !visited[rightIdx]) {
                visited[rightIdx] = true;
                queue.add(rightIdx);
            }
        }

        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int cy = idx / w;
            int cx = idx % w;

            if (cy > 0) {
                int nIdx = idx - w;
                if (!visited[nIdx] && isWhite[nIdx]) {
                    visited[nIdx] = true;
                    queue.add(nIdx);
                }
            }
            if (cy < h - 1) {
                int nIdx = idx + w;
                if (!visited[nIdx] && isWhite[nIdx]) {
                    visited[nIdx] = true;
                    queue.add(nIdx);
                }
            }
            if (cx > 0) {
                int nIdx = idx - 1;
                if (!visited[nIdx] && isWhite[nIdx]) {
                    visited[nIdx] = true;
                    queue.add(nIdx);
                }
            }
            if (cx < w - 1) {
                int nIdx = idx + 1;
                if (!visited[nIdx] && isWhite[nIdx]) {
                    visited[nIdx] = true;
                    queue.add(nIdx);
                }
            }
        }

        for (int i = 0; i < pixels.length; i++) {
            if (visited[i]) {
                pixels[i] = 0x00000000;
            }
        }

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    public static byte[] buildZipBundle(byte[] sheetPngBytes, List<SlicedSticker> stickers) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);

        ZipEntry sheetEntry = new ZipEntry("step3_sticker_sheet.png");
        zos.putNextEntry(sheetEntry);
        zos.write(sheetPngBytes);
        zos.closeEntry();

        for (SlicedSticker st : stickers) {
            ZipEntry entry = new ZipEntry("stickers/" + st.filename);
            zos.putNextEntry(entry);
            zos.write(st.pngBytes);
            zos.closeEntry();
        }
        zos.finish();
        zos.close();
        return bos.toByteArray();
    }

    public static byte[] bitmapToPngBytes(Bitmap bmp) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    public static byte[] bitmapToJpegBytes(Bitmap bmp, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        return bos.toByteArray();
    }

    private static String sanitizeFilename(String s) {
        if (s == null || s.trim().isEmpty()) return "sticker";
        String cleaned = s.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        if (cleaned.length() > 24) {
            cleaned = cleaned.substring(0, 24);
        }
        return cleaned.isEmpty() ? "sticker" : cleaned;
    }

    private static String postJson(String urlStr, String jsonBody, int timeoutMs) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(timeoutMs);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] outBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        OutputStream os = conn.getOutputStream();
        os.write(outBytes);
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        String respBody = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + ": " + extractErrorMessage(respBody));
        }
        return respBody;
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }

    private static String extractErrorMessage(String body) {
        if (body == null || body.isEmpty()) return "Unknown error";
        try {
            JSONObject obj = new JSONObject(body);
            JSONObject err = obj.optJSONObject("error");
            if (err != null) {
                return err.optString("message", body);
            }
        } catch (Exception ignored) {
        }
        return body.length() > 240 ? body.substring(0, 240) : body;
    }
}
