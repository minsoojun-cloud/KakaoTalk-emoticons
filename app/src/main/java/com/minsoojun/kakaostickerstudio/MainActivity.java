package com.minsoojun.kakaostickerstudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 100% Pure Native Android UI Activity (Zero WebView, Zero Cloud Run dependency).
 * Version 3.0:
 * - Prominent Gemini API Key setup card at the top
 * - 10 Mood Presets (including 🎣 낚시 테마 and ✈️ 여행 테마)
 * - 3 Theme Modes in Step 3 (💬 기본 일상 테마 / 🎣 낚시 테마 / ✈️ 여행 테마)
 *   with 🐟 Fishing Target Species selector (광어, 참돔, 돌돔, 갈치, 문어, 부리)
 *   and fishing actions + big-catch photo poses
 * - 5 Languages (한국어, English, 日本語, 中文, Español) across all themes
 * - Re-open Share/Email chooser button ("📤 완성된 이모티콘 공유 화면 다시 열기")
 *   available anytime after completion.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "kakao_sticker_native_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_PREFERRED_MODEL = "preferred_model";

    private static final int REQ_NATIVE_CAMERA = 1002;
    private static final int REQ_NATIVE_GALLERY = 1003;
    private static final int REQ_PERMISSIONS = 2001;

    private static final int COLOR_KAKAO_YELLOW = Color.parseColor("#FEE500");
    private static final int COLOR_KAKAO_BROWN = Color.parseColor("#3C1E1E");
    private static final int COLOR_BG_GRAY = Color.parseColor("#F7F8FA");
    private static final int COLOR_BORDER = Color.parseColor("#E2E8F0");
    private static final int COLOR_TEXT_MAIN = Color.parseColor("#191919");
    private static final int COLOR_TEXT_SUB = Color.parseColor("#64748B");

    // 1. Default Daily 15 scene compositions
    public static final String[] DEFAULT_SCENES = new String[]{
            "배를 잡고 눈물 나게 웃기",
            "지친 상대의 어깨를 따뜻하게 토닥여주기",
            "큰 땀방울을 흘리며 시무룩하게 고개 숙여 사과하기",
            "큰 하트와 함께 엄지척",
            "불타는 불꽃 오라와 함께 팔 근육 불끈 파이팅",
            "하트 눈으로 손하트를 날리며 주변에 분홍 하트 띄우기",
            "땀을 뻘뻘 흘리며 하늘색 아이스크림 하드 들기",
            "푹신한 흰 베개에 누워 하품하기",
            "머리에 귀여운 왕관을 쓰고 양손 엄지척과 반짝이",
            "폭죽이 터지는 배경에서 양팔 들고 신나게 점프",
            "반짝이와 함께 밝게 손 흔들며 인사",
            "한 손엔 응원봉, 한 손엔 깃발을 들고 신나게 응원",
            "포크와 맛있는 도시락을 들고 행복해하기",
            "스마트폰을 들고 체크 표시와 OK",
            "꿀단지에서 꿀을 떠먹으며 눈이 반짝이는 모습"
    };

    // 2. Fishing Theme 15 scene compositions (낚시하는 모습 + 큰 생선 잡았을 때 사진 포즈 잡는 모습 + 광어/참돔/돌돔/갈치/문어/부리)
    public static final String[] FISHING_SCENES = new String[]{
            "구명조끼와 낚시 모자를 쓰고 낚싯대와 아이스박스를 메고 신나게 낚시하러 출조하는 모습",
            "물가에서 낚싯대를 두 손으로 잡고 초릿대 끝을 진지하게 응시하며 입질을 기다리는 낚시 모습",
            "낚싯대가 활처럼 크게 휘어지며 대물 입질에 힘차게 챔질(히트!)하며 파이팅하는 역동적인 낚시 모습",
            "잡아 올린 아주 큰 대물 광어를 두 손으로 번쩍 들고 카메라를 보며 자랑스럽게 인증 사진 포즈 잡는 모습",
            "붉고 영롱한 큰 대물 참돔을 품에 안고 환호하며 카메라를 향해 인증 사진 포즈 잡는 모습",
            "선명한 검은 줄무늬의 큰 대물 돌돔을 한 손에 높이 들고 엄지척하며 당당하게 사진 포즈 잡는 모습",
            "길고 은빛으로 반짝이는 큰 대물 갈치를 높이 들어 올리며 신나게 인증 사진 포즈 잡는 모습",
            "다리가 굵은 큰 대왕 문어를 들어 올리며 깜짝 놀라고 기뻐하며 인증 사진 포즈 잡는 모습",
            "몸집만 한 거대한 부리(대방어/부시리)를 두 팔로 힘껏 안아 들고 위풍당당하게 사진 포즈 잡는 모습",
            "큰 물고기가 수면 위로 올라와 땀을 뻘뻘 흘리며 다급하게 뜰채를 외치며 낚시하는 모습",
            "광어·참돔·돌돔·갈치·문어·부리가 가득 찬 만쿨 아이스박스 옆에서 왕관을 쓰고 양손 엄지척 포즈",
            "바늘털이로 큰 물고기를 눈앞에서 놓치고 무릎을 꿇으며 눈물 흘리는 아쉬운 모습",
            "낚시 의자에 앉아 커피를 마시며 여유롭게 낚싯줄을 드리우고 평화롭게 힐링 낚시하는 모습",
            "낚싯배 위에서 큰 생선을 들고 하트와 반짝이 속에서 선장님께 쌍따봉 사진 포즈 잡는 모습",
            "직접 잡은 싱싱한 생선회 한 접시와 초장을 들고 행복하게 건배하는 모습"
    };

    // 3. Travel Theme 15 scene compositions (설레는 여행 출발 + 명소 인증샷 포즈 + 맛집 + 힐링)
    public static final String[] TRAVEL_SCENES = new String[]{
            "캐리어를 끌고 여권과 비행기 탑승권을 흔들며 설레는 표정으로 공항으로 출발하는 모습",
            "비행기 창가 좌석에 앉아 창밖 구름을 보며 두근두근 설레는 미소를 짓는 모습",
            "야자수와 맑은 하늘 아래 선글라스를 끼고 양팔을 벌려 여행지 바람을 만끽하는 모습",
            "유명 여행지 랜드마크 앞에서 카메라를 메고 멋지게 브이(V) 인증샷 사진 포즈 잡는 모습",
            "식탁 가득 차려진 맛있는 현지 음식을 들고 하트 눈으로 행복해하는 모습",
            "여행 지도를 들고 땀방울을 흘리며 어리둥절하게 길을 찾는 귀여운 모습",
            "양손 가득 기념품과 쇼핑백을 들고 선글라스를 낀 채 신나게 걸어가는 모습",
            "휴양지 해변 파라솔 아래 선베드에 누워 시원한 음료를 마시며 여유롭게 힐링하는 모습",
            "스마트폰으로 방금 찍은 여행 사진을 확인하며 반짝이와 함께 엄지척 감탄하는 모습",
            "하루 종일 여행하고 지쳐서 숙소 침대에 털썩 엎드려 쉬는 모습",
            "반짝이는 밤하늘과 아름다운 야경 불빛을 바라보며 두 손을 모으고 감동하는 모습",
            "시원한 음료/맥주잔을 높이 들고 폭죽이 터지는 배경에서 신나게 건배하는 모습",
            "예쁜 여행지 기념품 상자를 품에 안고 사랑스러운 윙크와 손하트를 보내는 모습",
            "귀국길에 캐리어를 꼭 끌어안고 돌아가기 싫어 눈물을 흘리는 코믹한 모습",
            "추억이 담긴 여행 사진들을 들고 밝게 손 흔들며 다음 여행을 기약하는 모습"
    };

    public static final String[] FISHING_SPECIES = new String[]{
            "전체 (광어·참돔·돌돔·갈치·문어·부리)",
            "광어",
            "참돔",
            "돌돔",
            "갈치",
            "문어",
            "부리"
    };

    public static class MoodPreset {
        public final String id;
        public final String icon;
        public final String label;
        public final String subtitle;
        public final String badge;
        public final String stylePrompt;

        public MoodPreset(String id, String icon, String label, String subtitle, String badge, String stylePrompt) {
            this.id = id;
            this.icon = icon;
            this.label = label;
            this.subtitle = subtitle;
            this.badge = badge;
            this.stylePrompt = stylePrompt;
        }
    }

    public static class LanguagePreset {
        public final String id;
        public final String flag;
        public final String label;
        public final String fontInstruction;
        public final String[] defaultPhrases;
        public final String[] fishingPhrases;
        public final String[] travelPhrases;

        public LanguagePreset(
                String id,
                String flag,
                String label,
                String fontInstruction,
                String[] defaultPhrases,
                String[] fishingPhrases,
                String[] travelPhrases) {
            this.id = id;
            this.flag = flag;
            this.label = label;
            this.fontInstruction = fontInstruction;
            this.defaultPhrases = defaultPhrases;
            this.fishingPhrases = fishingPhrases;
            this.travelPhrases = travelPhrases;
        }

        public String[] getPhrasesForTheme(String themeId) {
            if ("fishing".equals(themeId)) return fishingPhrases;
            if ("travel".equals(themeId)) return travelPhrases;
            return defaultPhrases;
        }
    }

    // 10 Mood Presets (8 existing + 🎣 낚시 테마 + ✈️ 여행 테마)
    private static final List<MoodPreset> MOOD_PRESETS = Arrays.asList(
            new MoodPreset("cute", "🥰", "귀엽게", "동글동글 친근한 일상 SD", "인기 1위",
                    "귀엽고 친근한 2D 카카오톡 스타일(동글동글한 2.5등신 비율과 발그레한 볼터치)로 표현해줘. 전체적으로 밝고 사랑스럽고 친근한 데일리 메신저 이모티콘 느낌으로 완성해줘."),
            new MoodPreset("energetic", "🎉", "발랄하게", "하이텐션 & 리액션 폭발", "텐션 업",
                    "발랄하고 에너지가 넘치는 하이텐션 2D 카카오톡 SD 스타일로 표현해줘. 역동적인 점프와 활기찬 손동작, 반짝이·폭죽·음표·팝아트 효과를 풍성하게 넣어 통통 튀고 신나는 분위기로 완성해줘."),
            new MoodPreset("serious", "🧐", "진지하게", "결연한 눈빛 & 반전 매력", "반전 개그",
                    "진지하고 비장미 넘치는 표정(진한 눈썹, 결연한 눈빛, 살짝 힘준 입매)과 귀여운 2.5등신 SD 비율이 대비되는 반전 매력 카카오톡 이모티콘 스타일로 표현해줘."),
            new MoodPreset("comic", "🤪", "코믹/익살스럽게", "과장된 표정 & 개그 유머", "웃음 보장",
                    "코믹하고 익살스러운 개그 만화풍 2D 카카오톡 SD 스타일로 표현해줘. 표정 근육과 눈코입을 아주 크고 익살스럽게 과장하고 유쾌한 코믹 이펙트를 적극 활용해줘."),
            new MoodPreset("office", "💼", "직장인 공감", "예의 바른 사회생활 필수템", "단톡방 추천",
                    "직장인·팀 단톡방에서 쓰기 좋은 깔끔하고 센스 있는 2D 카카오톡 SD 스타일로 표현해줘. 정중하면서도 공감 가는 리액션이 한눈에 들어오도록 완성해줘."),
            new MoodPreset("warm", "🌷", "따뜻하고 감성적으로", "포근한 파스텔 힐링 무드", "힐링 감성",
                    "따뜻하고 포근한 파스텔 톤 감성 2D 카카오톡 SD 일러스트 스타일로 표현해줘. 부드러운 색감과 온화한 미소, 포근한 꽃잎·별빛 효과로 다정한 느낌을 강조해줘."),
            new MoodPreset("lovely", "💖", "애교 가득 러블리", "하트 뿜뿜 커플·가족용", "러블리",
                    "애교가 넘치고 사랑스러운 러블리 2D 카카오톡 SD 스타일로 표현해줘. 초롱초롱한 눈망울과 핑크빛 볼터치, 하트와 리본 효과를 풍성하게 담아줘."),
            new MoodPreset("hip", "😎", "힙 & 트렌디하게", "MZ 감성 스트릿 팝 스타일", "트렌디",
                    "힙하고 트렌디한 팝 일러스트 감성의 2D 카카오톡 SD 스타일로 표현해줘. 선명하고 감각적인 컬러감과 쿨하고 당당한 제스처로 세련된 메신저 스티커 느낌을 살려줘."),
            new MoodPreset("fishing", "🎣", "낚시 테마", "낚시 모습 & 대물 인증샷 포즈", "강태공 필수",
                    "열정 넘치고 생동감 있는 낚시꾼(강태공) 2D 카카오톡 SD 이모티콘 스타일로 표현해줘. 구명조끼·낚시 모자·낚시 조끼 차림으로 낚싯대 캐스팅과 입질 대기, 짜릿한 챔질(히트!) 하는 모습과 광어·참돔·돌돔·갈치·문어·부리(대방어/부시리) 등 큰 생선을 잡았을 때 두 손으로 들고 자랑스럽게 사진 포즈(인증샷 포즈)를 잡는 모습을 역동적이고 유쾌하게 완성해줘."),
            new MoodPreset("travel", "✈️", "여행 테마", "설레는 출발·인증샷·맛집·힐링", "여행 필수",
                    "설레고 자유로운 여행자 감성의 2D 카카오톡 SD 이모티콘 스타일로 표현해줘. 캐리어·여권·카메라·선글라스·배낭 등 여행 소품과 함께 공항 출발, 유명 명소 인증샷 포즈, 현지 맛집 탐방, 휴양지 힐링 등 즐겁고 생생한 여행 리액션으로 완성해줘.")
    );

    private static final List<LanguagePreset> LANGUAGE_PRESETS = Arrays.asList(
            new LanguagePreset(
                    "ko", "🇰🇷", "한국어",
                    "글씨는 굵고 가독성 높은 둥근 한글 손글씨/고딕 폰트 스타일로 정확하고 또렷하게 써줘.",
                    new String[]{"ㅋㅋㅋㅋ", "오늘도 고생 많았어요", "미안해요", "좋아요", "힘내요", "사랑해요", "더워요", "피곤해요", "최고", "오예", "안녕", "응원해요", "맛점하세요", "확인완료", "꿀잼"},
                    new String[]{"출조 가즈아!", "입질 온다 집중!", "히트다 히트!", "대물 광어 잡았다!", "참돔 손맛 최고!", "전설의 돌돔 인증!", "은빛 갈치 대박!", "대왕 문어 히트!", "몬스터 부리 제압!", "뜰채 빨리 가져와!", "오늘 만쿨 확정!", "놓쳤다 아까비..", "힐링 낚시 중~", "선장님 포인트 최고!", "오늘 저녁은 회 파티!"},
                    new String[]{"여행 출발!", "비행기 탑승 완료", "도착! 날씨 최고", "여긴 꼭 찍어야 해", "현지 맛집 대성공", "길 잃었어 어디지?", "쇼핑 플렉스!", "완전 힐링 중~", "인생샷 건졌다!", "체력 방전 숙소로..", "야경 너무 예쁘다", "건배! 여행 최고", "선물 사갈게!", "돌아가기 싫어요", "다음에 또 가자!"}
            ),
            new LanguagePreset(
                    "en", "🇺🇸", "English",
                    "Write the English text in bold, cute, rounded comic lettering with high legibility above each character.",
                    new String[]{"LOL", "Great job today!", "So sorry!", "Love it!", "You got this!", "Love you!", "So hot!", "So tired...", "The Best!", "Oh Yeah!", "Hello!", "Cheering for you!", "Enjoy lunch!", "Checked!", "So fun!"},
                    new String[]{"Gone Fishing!", "Wait for the bite!", "FISH ON! HIT!", "Huge Flounder!", "Red Seabream!", "Monster Rockbream!", "Silver Hairtail!", "Giant Octopus!", "Big Amberjack!", "Get the net!", "Full Cooler!", "It got away..", "Chill Fishing~", "Best Captain!", "Sashimi Party!"},
                    new String[]{"Trip Time!", "On Board!", "Arrived! Sunny!", "Photo Spot!", "Yummy Local Food!", "Where am I?", "Shopping Spree!", "Pure Healing~", "Best Shot Ever!", "So Exhausted..", "Amazing Night View!", "Cheers! Best Trip!", "Got Souvenirs!", "Don't wanna leave", "Let's go again!"}
            ),
            new LanguagePreset(
                    "ja", "🇯🇵", "日本語",
                    "文字は太くて読みやすい丸ゴシック・手書き風の日本語フォントで、キャラクターの頭上に正確かつ鮮明に配置してください。",
                    new String[]{"wwww", "今日もお疲れ様！", "ごめんね", "いいね！", "ファイト！", "大好き！", "暑い〜", "疲れた…", "最高！", "やったー！", "こんにちは！", "応援してるよ！", "ランチ楽しんで！", "確認完了！", "超ウケる！"},
                    new String[]{"釣りに出発！", "アタリ待ち集中！", "ヒット！きたー！", "座布団ヒラメ釣れた！", "大物マダイ最高！", "幻のイシダイ！", "銀太刀魚大漁！", "大ダコゲット！", "巨大ブリ制覇！", "タモ網お願い！", "クーラー満タン！", "バラした悔しい…", "のんびり釣り中♪", "船長ポイント最高！", "今夜は刺身パーティー！"},
                    new String[]{"旅行に出発！", "搭乗完了！", "到着！天気最高", "ここで記念撮影！", "ご当地グルメ最高", "道に迷った…", "爆買いモード！", "最高の癒やし〜", "神ショット撮れた！", "体力ゼロでホテルへ", "夜景がキレイ！", "乾杯！最高の旅", "お土産買ったよ！", "帰りたくない〜", "また行こうね！"}
            ),
            new LanguagePreset(
                    "zh", "🇨🇳", "中文",
                    "文字请使用粗体、圆润可爱的中文手写/圆体字风格，清晰准确地写在每个角色头顶上方。",
                    new String[]{"哈哈哈", "今天辛苦啦", "对不起", "太赞了", "加油！", "爱你哟", "好热啊", "好累哦", "最棒！", "太棒啦！", "你好呀", "为你打气", "午餐愉快", "已确认", "太有趣了"},
                    new String[]{"出发去钓鱼！", "屏住呼吸等咬钩", "中鱼啦！大咬！", "钓到大比目鱼！", "真鲷手感绝了！", "梦幻条石鲷！", "银亮大带鱼！", "抓到大章鱼！", "巨型鰤鱼拿下！", "快拿抄网来！", "今天爆箱啦！", "脱钩跑鱼好可惜", "悠闲垂钓中~", "船长钓点太棒！", "今晚生鱼片大餐！"},
                    new String[]{"出发去旅行！", "顺利登机啦", "抵达！天气超好", "打卡拍照留念", "当地美食太赞", "迷路了在哪儿？", "买买买太开心！", "度假太治愈啦~", "拍到人生照片！", "累瘫回酒店啦", "夜景太美了吧", "干杯！旅行最棒", "带了伴手礼哦！", "不想回家呀", "下次还要一起去！"}
            ),
            new LanguagePreset(
                    "es", "🇪🇸", "Español",
                    "Escribe el texto en español con letras redondeadas, gruesas y muy legibles encima de la cabeza de cada personaje.",
                    new String[]{"Jajaja", "¡Buen trabajo!", "¡Perdón!", "¡Me encanta!", "¡Ánimo!", "¡Te quiero!", "¡Qué calor!", "Qué sueño...", "¡El mejor!", "¡Genial!", "¡Hola!", "¡Tú puedes!", "¡Buen provecho!", "¡Listo!", "¡Qué divertido!"},
                    new String[]{"¡Vamos de pesca!", "¡Esperando picada!", "¡Picó uno grande!", "¡Gran Lenguado!", "¡Súper Pargo Rojo!", "¡Pargo de Roca!", "¡Pez Sable gigante!", "¡Pulpo Gigante!", "¡Gran Pez Limón!", "¡Trae la red ya!", "¡Nevera llena!", "¡Se escapó casi!", "Pesca tranquila~", "¡Gran Capitán!", "¡Fiesta de Sashimi!"},
                    new String[]{"¡Nos vamos de viaje!", "¡Listo en el avión!", "¡Llegamos! Qué sol", "¡Foto aquí sí o sí!", "¡Comida deliciosa!", "¿Dónde estoy?", "¡De compras!", "¡Relax total~!", "¡Fotaza perfecta!", "Agotado al hotel..", "¡Qué vista nocturna!", "¡Salud! Gran viaje", "¡Compré regalos!", "¡No quiero volver!", "¡Volvamos pronto!"}
            )
    );

    private static final String[] MODEL_IDS = new String[]{
            "gemini-3-pro-image-preview",
            "gemini-3.1-flash-image-preview",
            "gemini-2.5-flash-image",
            "gemini-2.0-flash-exp-image-generation"
    };

    private static final String[] MODEL_LABELS = new String[]{
            "✨ gemini-3-pro-image-preview (고품질 한글 추천)",
            "⚡ gemini-3.1-flash-image-preview (고속 생성)",
            "🚀 gemini-2.5-flash-image (기본 이미지 모델)",
            "🧪 gemini-2.0-flash-exp-image-generation (호환용)"
    };

    // State
    private String selectedMoodId = "cute";
    private String selectedThemeId = "default"; // "default", "fishing", "travel"
    private String selectedFishTarget = FISHING_SPECIES[0];
    private String selectedLanguageId = "ko";
    private String selectedBgMode = "white";
    private int selectedStickerSize = 108;
    private byte[] selectedPhotoBytes = null;
    private boolean isUpdatingPhrasesProgrammatically = false;
    private boolean isPasswordVisible = false;

    private Uri cameraPhotoUri;
    private File cameraPhotoFile;
    private boolean pendingNativeCameraLaunch = false;

    // Cached generation results for instant offline white/transparent switching & re-opening share sheet
    private Bitmap lastRawSheetBitmap = null;
    private List<String> lastPhrases = new ArrayList<>();
    private String lastMoodLabel = "귀엽게";
    private String lastLanguageLabel = "한국어";
    private String lastUsedModel = "gemini-3-pro-image-preview";
    private OnDeviceStickerEngine.SliceResult lastSliceResult = null;

    // Native UI References
    private ScrollView mainScrollView;
    private EditText apiKeyInput;
    private Spinner modelSpinner;
    private TextView apiKeyStatusView;
    private ImageView photoPreviewImage;
    private TextView photoPlaceholderText;
    private final Map<String, LinearLayout> moodCardViews = new LinkedHashMap<>();
    private EditText customMoodInput;
    private final Map<String, TextView> themeTabViews = new LinkedHashMap<>();
    private LinearLayout fishingSpeciesContainer;
    private final Map<String, TextView> fishingSpeciesChipViews = new LinkedHashMap<>();
    private final Map<String, TextView> langTabViews = new LinkedHashMap<>();
    private final EditText[] phraseInputs = new EditText[15];
    private final EditText[] sceneInputs = new EditText[15];
    private final LinearLayout[] phraseCells = new LinearLayout[15];
    private GridLayout phrasesGrid;
    private boolean isSingleColumnPoseEditMode = false;
    private CheckBox autoPromptCheckBox;
    private EditText promptEditText;
    private LinearLayout bgWhiteCard;
    private LinearLayout bgTransparentCard;
    private final Map<Integer, TextView> sizeBtnViews = new LinkedHashMap<>();
    private EditText emailInput;
    private Button generateBtn;
    private Button stickyReopenShareBtn;

    // Results section views
    private LinearLayout resultSectionCard;
    private TextView resultMetaBadge;
    private TextView resultStatusBanner;
    private FrameLayout sheetPreviewContainer;
    private ImageView resultSheetImageView;
    private GridLayout resultStickersGrid;
    private EditText resendEmailInput;

    // Loading dialog views
    private AlertDialog progressDialog;
    private TextView progressTitleView;
    private TextView progressDescView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildNativeUi();
        loadSavedSettings();
        refreshMoodSelectionUi();
        refreshThemeTabsUi();
        refreshFishingSpeciesChipsUi();
        refreshLanguageTabsUi();
        refreshPromptIfAuto();
        requestInitialPermissionsIfNeeded();
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private GradientDrawable roundedBox(int fillColor, int strokeColor, int strokeWidthDp, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0) {
            gd.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return gd;
    }

    private Drawable createCheckerboardDrawable() {
        return new Drawable() {
            private final Paint lightPaint = new Paint();
            private final Paint darkPaint = new Paint();
            {
                lightPaint.setColor(Color.WHITE);
                darkPaint.setColor(Color.parseColor("#E2E8F0"));
            }

            @Override
            public void draw(Canvas canvas) {
                int cell = dp(8);
                int w = getBounds().width();
                int h = getBounds().height();
                canvas.drawRect(getBounds(), lightPaint);
                for (int y = 0; y < h; y += cell) {
                    for (int x = 0; x < w; x += cell) {
                        if (((x / cell) + (y / cell)) % 2 == 1) {
                            canvas.drawRect(x, y, Math.min(w, x + cell), Math.min(h, y + cell), darkPaint);
                        }
                    }
                }
            }

            @Override
            public void setAlpha(int alpha) {}

            @Override
            public void setColorFilter(ColorFilter colorFilter) {}

            @Override
            public int getOpacity() {
                return PixelFormat.OPAQUE;
            }
        };
    }

    private void buildNativeUi() {
        LinearLayout rootScreen = new LinearLayout(this);
        rootScreen.setOrientation(LinearLayout.VERTICAL);
        rootScreen.setBackgroundColor(COLOR_BG_GRAY);
        rootScreen.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Top Native App Header Bar (Kakao Yellow)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(COLOR_KAKAO_YELLOW);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView logoBadge = new TextView(this);
        logoBadge.setText("톡");
        logoBadge.setTextColor(COLOR_KAKAO_YELLOW);
        logoBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        logoBadge.setTypeface(Typeface.DEFAULT_BOLD);
        logoBadge.setGravity(Gravity.CENTER);
        logoBadge.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 10));
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        logoLp.rightMargin = dp(10);
        header.addView(logoBadge, logoLp);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView titleTv = new TextView(this);
        titleTv.setText("AI 카카오톡 이모티콘 스튜디오");
        titleTv.setTextColor(COLOR_KAKAO_BROWN);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        TextView subTv = new TextView(this);
        subTv.setText("100% 독립 실행형 Native 앱 (v3.1 · 문구/포즈 개별 편집)");
        subTv.setTextColor(Color.parseColor("#5C3A3A"));
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleCol.addView(titleTv);
        titleCol.addView(subTv);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView pill = new TextView(this);
        pill.setText("v3.1 Native");
        pill.setTextColor(COLOR_KAKAO_YELLOW);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        pill.setTypeface(Typeface.DEFAULT_BOLD);
        pill.setPadding(dp(10), dp(5), dp(10), dp(5));
        pill.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 99));
        header.addView(pill);

        rootScreen.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. Scrollable Content Area
        mainScrollView = new ScrollView(this);
        mainScrollView.setFillViewport(true);
        LinearLayout contentList = new LinearLayout(this);
        contentList.setOrientation(LinearLayout.VERTICAL);
        contentList.setPadding(dp(14), dp(14), dp(14), dp(24));

        // --- CARD 0: GEMINI API KEY SETTING (Prominent Yellow Highlight Card at Very Top) ---
        LinearLayout apiCard = createSectionCard("#FEFCE8", "#FACC15");
        apiCard.addView(createSectionHeader("🔑", "Gemini API 키 설정 (필수 · 기기 내 저장)", "외부 서버 없이 앱 자체 동작"));

        LinearLayout keyInputRow = new LinearLayout(this);
        keyInputRow.setOrientation(LinearLayout.HORIZONTAL);
        keyInputRow.setGravity(Gravity.CENTER_VERTICAL);

        apiKeyInput = new EditText(this);
        apiKeyInput.setHint("여기에 Gemini API 키 입력 (AIza...)");
        apiKeyInput.setSingleLine(true);
        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKeyInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        apiKeyInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        apiKeyInput.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        keyLp.rightMargin = dp(6);
        keyInputRow.addView(apiKeyInput, keyLp);

        final Button toggleKeyBtn = new Button(this);
        toggleKeyBtn.setText("보기");
        toggleKeyBtn.setAllCaps(false);
        toggleKeyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        toggleKeyBtn.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        toggleKeyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    toggleKeyBtn.setText("숨김");
                } else {
                    apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    toggleKeyBtn.setText("보기");
                }
                apiKeyInput.setSelection(apiKeyInput.getText().length());
            }
        });
        keyInputRow.addView(toggleKeyBtn, new LinearLayout.LayoutParams(dp(64), dp(42)));
        apiCard.addView(keyInputRow);

        modelSpinner = new Spinner(this);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, MODEL_LABELS);
        modelSpinner.setAdapter(spinnerAdapter);
        modelSpinner.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spinLp.topMargin = dp(8);
        apiCard.addView(modelSpinner, spinLp);

        LinearLayout keyBtnRow = new LinearLayout(this);
        keyBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams keyBtnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        keyBtnRowLp.topMargin = dp(8);

        Button saveKeyBtn = new Button(this);
        saveKeyBtn.setText("💾 API 키 기기에 저장");
        saveKeyBtn.setAllCaps(false);
        saveKeyBtn.setTextColor(COLOR_KAKAO_YELLOW);
        saveKeyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveKeyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        saveKeyBtn.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 10));
        saveKeyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentSettings();
                showApiKeyStatus(true, "✅ Gemini API 키와 모델 설정이 스마트폰 내부에 저장되었습니다.");
            }
        });
        LinearLayout.LayoutParams saveBtnLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        saveBtnLp.rightMargin = dp(6);
        keyBtnRow.addView(saveKeyBtn, saveBtnLp);

        Button verifyKeyBtn = new Button(this);
        verifyKeyBtn.setText("🔍 API 키 연결 테스트");
        verifyKeyBtn.setAllCaps(false);
        verifyKeyBtn.setTextColor(COLOR_TEXT_MAIN);
        verifyKeyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        verifyKeyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        verifyKeyBtn.setBackground(roundedBox(Color.WHITE, COLOR_KAKAO_BROWN, 1, 10));
        verifyKeyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String key = apiKeyInput.getText().toString().trim();
                if (key.isEmpty()) {
                    showApiKeyStatus(false, "❌ 먼저 Gemini API 키(AIza...)를 입력해 주세요.");
                    return;
                }
                saveCurrentSettings();
                showApiKeyStatus(true, "⏳ Gemini API 서버와 키 유효성을 직접 확인 중입니다...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final JSONObject res = OnDeviceStickerEngine.verifyApiKey(key);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showApiKeyStatus(res.optBoolean("ok", false), res.optString("message", ""));
                            }
                        });
                    }
                }).start();
            }
        });
        keyBtnRow.addView(verifyKeyBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        apiCard.addView(keyBtnRow, keyBtnRowLp);

        apiKeyStatusView = new TextView(this);
        apiKeyStatusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        apiKeyStatusView.setPadding(dp(10), dp(8), dp(10), dp(8));
        apiKeyStatusView.setVisibility(View.GONE);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(8);
        apiCard.addView(apiKeyStatusView, statusLp);

        contentList.addView(apiCard, cardMarginParams());

        // --- CARD 1: PHOTO UPLOAD & CAMERA CAPTURE ---
        LinearLayout photoCard = createSectionCard("#FFFFFF", "#E5E8EB");
        photoCard.addView(createSectionHeader("1", "인물 사진 업로드 / 카메라 촬영", "정면·상반신 권장"));

        FrameLayout previewBox = new FrameLayout(this);
        previewBox.setBackground(roundedBox(Color.parseColor("#F8FAFC"), Color.parseColor("#CBD5E1"), 2, 12));
        previewBox.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams previewBoxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        previewBoxLp.bottomMargin = dp(10);

        photoPlaceholderText = new TextView(this);
        photoPlaceholderText.setText("🤳\n사진 앨범에서 선택하거나 카메라로 촬영해 주세요\n(기기 내부에서 인물 중앙 크롭 및 배경 분리 수행)");
        photoPlaceholderText.setGravity(Gravity.CENTER);
        photoPlaceholderText.setTextColor(COLOR_TEXT_SUB);
        photoPlaceholderText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        previewBox.addView(photoPlaceholderText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        photoPreviewImage = new ImageView(this);
        photoPreviewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoPreviewImage.setVisibility(View.GONE);
        previewBox.addView(photoPreviewImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        photoCard.addView(previewBox, previewBoxLp);

        LinearLayout photoBtns = new LinearLayout(this);
        photoBtns.setOrientation(LinearLayout.HORIZONTAL);

        Button galleryBtn = new Button(this);
        galleryBtn.setText("🖼️ 사진 앨범 선택");
        galleryBtn.setAllCaps(false);
        galleryBtn.setTypeface(Typeface.DEFAULT_BOLD);
        galleryBtn.setTextColor(COLOR_TEXT_MAIN);
        galleryBtn.setBackground(roundedBox(Color.parseColor("#F1F5F9"), Color.parseColor("#CBD5E1"), 1, 12));
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchNativeGalleryPicker();
            }
        });
        LinearLayout.LayoutParams galLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        galLp.rightMargin = dp(8);
        photoBtns.addView(galleryBtn, galLp);

        Button cameraBtn = new Button(this);
        cameraBtn.setText("📷 카메라로 사진 찍기");
        cameraBtn.setAllCaps(false);
        cameraBtn.setTypeface(Typeface.DEFAULT_BOLD);
        cameraBtn.setTextColor(COLOR_KAKAO_YELLOW);
        cameraBtn.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 12));
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchNativeCameraCapture();
            }
        });
        photoBtns.addView(cameraBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));
        photoCard.addView(photoBtns);

        contentList.addView(photoCard, cardMarginParams());

        // --- CARD 2: MOOD SELECTOR (10 PRESETS INCLUDING 🎣 낚시 테마 & ✈️ 여행 테마) ---
        LinearLayout moodCard = createSectionCard("#FFFFFF", "#E5E8EB");
        moodCard.addView(createSectionHeader("2", "전체 스티커 분위기 선택", "기존 8종 + 낚시·여행 테마"));

        GridLayout moodGrid = new GridLayout(this);
        moodGrid.setColumnCount(2);
        for (int i = 0; i < MOOD_PRESETS.size(); i++) {
            final MoodPreset mp = MOOD_PRESETS.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(10), dp(10), dp(10), dp(10));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedMoodId = mp.id;
                    refreshMoodSelectionUi();
                    // Automatically synchronize Step 3 Theme if user picks Fishing or Travel mood
                    if ("fishing".equals(mp.id)) {
                        selectThemeMode("fishing");
                    } else if ("travel".equals(mp.id)) {
                        selectThemeMode("travel");
                    } else {
                        refreshPromptIfAuto();
                    }
                }
            });

            TextView topRow = new TextView(this);
            topRow.setText(mp.icon + " " + mp.label + "  [" + mp.badge + "]");
            topRow.setTextColor(COLOR_TEXT_MAIN);
            topRow.setTypeface(Typeface.DEFAULT_BOLD);
            topRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);

            TextView subRow = new TextView(this);
            subRow.setText(mp.subtitle);
            subRow.setTextColor(COLOR_TEXT_SUB);
            subRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

            item.addView(topRow);
            item.addView(subRow);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.columnSpec = GridLayout.spec(i % 2, 1, 1f);
            glp.setMargins(dp(4), dp(4), dp(4), dp(4));
            moodGrid.addView(item, glp);
            moodCardViews.put(mp.id, item);
        }
        moodCard.addView(moodGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        customMoodInput = new EditText(this);
        customMoodInput.setHint("추가 분위기 직접 입력 (선택: 예 - 파란 모자 착용, 더 과장된 표정)");
        customMoodInput.setSingleLine(true);
        customMoodInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        customMoodInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        customMoodInput.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        customMoodInput.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void onChanged() {
                refreshPromptIfAuto();
            }
        });
        LinearLayout.LayoutParams cmLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cmLp.topMargin = dp(8);
        moodCard.addView(customMoodInput, cmLp);

        contentList.addView(moodCard, cardMarginParams());

        // --- CARD 3: 이모티콘 언어 & 15종 문구·포즈 편집 (기존 일상 테마 + 낚시 테마 + 여행 테마) ---
        LinearLayout langCard = createSectionCard("#FFFFFF", "#E5E8EB");
        langCard.addView(createSectionHeader("3", "이모티콘 언어 & 15종 문구·포즈 편집", "문구·포즈 모두 직접 수정 가능"));

        // 3-A. Theme Selector Row inside Step 3 (기본 일상 / 낚시 테마 / 여행 테마)
        TextView themeTitleTv = new TextView(this);
        themeTitleTv.setText("🎯 문구 및 상황 테마 선택 (선택 시 15종 문구·포즈 자동 전환)");
        themeTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        themeTitleTv.setTypeface(Typeface.DEFAULT_BOLD);
        themeTitleTv.setTextColor(COLOR_TEXT_MAIN);
        LinearLayout.LayoutParams ttLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttLp.bottomMargin = dp(6);
        langCard.addView(themeTitleTv, ttLp);

        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        String[][] themeDefs = new String[][]{
                {"default", "💬 기본 일상 테마"},
                {"fishing", "🎣 낚시 테마"},
                {"travel", "✈️ 여행 테마"}
        };
        for (int i = 0; i < themeDefs.length; i++) {
            final String tId = themeDefs[i][0];
            String tLabel = themeDefs[i][1];
            TextView tBtn = new TextView(this);
            tBtn.setText(tLabel);
            tBtn.setGravity(Gravity.CENTER);
            tBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tBtn.setTypeface(Typeface.DEFAULT_BOLD);
            tBtn.setPadding(dp(8), dp(9), dp(8), dp(9));
            tBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectThemeMode(tId);
                    if ("fishing".equals(tId)) {
                        selectedMoodId = "fishing";
                        refreshMoodSelectionUi();
                    } else if ("travel".equals(tId)) {
                        selectedMoodId = "travel";
                        refreshMoodSelectionUi();
                    }
                }
            });
            LinearLayout.LayoutParams tBtnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < themeDefs.length - 1) tBtnLp.rightMargin = dp(6);
            themeRow.addView(tBtn, tBtnLp);
            themeTabViews.put(tId, tBtn);
        }
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.bottomMargin = dp(10);
        langCard.addView(themeRow, trLp);

        // 3-B. Fishing Target Species Selector Box (광어, 참돔, 돌돔, 갈치, 문어, 부리)
        fishingSpeciesContainer = new LinearLayout(this);
        fishingSpeciesContainer.setOrientation(LinearLayout.VERTICAL);
        fishingSpeciesContainer.setPadding(dp(10), dp(10), dp(10), dp(10));
        fishingSpeciesContainer.setBackground(roundedBox(Color.parseColor("#EFF6FF"), Color.parseColor("#93C5FD"), 1, 10));
        fishingSpeciesContainer.setVisibility(View.GONE);

        TextView fishHeaderTv = new TextView(this);
        fishHeaderTv.setText("🐟 낚시 대상 어종 선택 (광어 · 참돔 · 돌돔 · 갈치 · 문어 · 부리)");
        fishHeaderTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fishHeaderTv.setTypeface(Typeface.DEFAULT_BOLD);
        fishHeaderTv.setTextColor(Color.parseColor("#1E3A8A"));
        fishingSpeciesContainer.addView(fishHeaderTv);

        TextView fishSubTv = new TextView(this);
        fishSubTv.setText("기본값은 6대 어종(광어·참돔·돌돔·갈치·문어·부리)이 15개 스티커에 모두 포함되며, 특정 어종을 탭하면 해당 어종 중심 대물 포즈로 문구를 맞춤 전환합니다.");
        fishSubTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        fishSubTv.setTextColor(Color.parseColor("#1E40AF"));
        LinearLayout.LayoutParams fSubLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fSubLp.topMargin = dp(2);
        fSubLp.bottomMargin = dp(6);
        fishingSpeciesContainer.addView(fishSubTv, fSubLp);

        HorizontalScrollView fishScroll = new HorizontalScrollView(this);
        fishScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout fishRow = new LinearLayout(this);
        fishRow.setOrientation(LinearLayout.HORIZONTAL);
        for (final String sp : FISHING_SPECIES) {
            TextView chip = new TextView(this);
            chip.setText("🐟 " + sp);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedFishTarget = sp;
                    refreshFishingSpeciesChipsUi();
                    applySingleFishFocusIfSelected(sp);
                    refreshPromptIfAuto();
                }
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.rightMargin = dp(6);
            fishRow.addView(chip, cLp);
            fishingSpeciesChipViews.put(sp, chip);
        }
        fishScroll.addView(fishRow);
        fishingSpeciesContainer.addView(fishScroll);

        LinearLayout.LayoutParams fscLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fscLp.bottomMargin = dp(10);
        langCard.addView(fishingSpeciesContainer, fscLp);

        // 3-C. Language Auto-Switch Tabs (한국어, English, 日本語, 中文, Español)
        HorizontalScrollView langScroll = new HorizontalScrollView(this);
        langScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        for (final LanguagePreset lp : LANGUAGE_PRESETS) {
            TextView tab = new TextView(this);
            tab.setText(lp.flag + " " + lp.label);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tab.setPadding(dp(14), dp(8), dp(14), dp(8));
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!selectedLanguageId.equals(lp.id)) {
                        selectedLanguageId = lp.id;
                        refreshLanguageTabsUi();
                        applyCurrentThemeAndLanguagePhrases();
                    }
                }
            });
            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tabLp.rightMargin = dp(6);
            langRow.addView(tab, tabLp);
            langTabViews.put(lp.id, tab);
        }
        langScroll.addView(langRow);
        LinearLayout.LayoutParams lsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lsLp.bottomMargin = dp(10);
        langCard.addView(langScroll, lsLp);

        // 3-D. Pose & Phrase Editing Toolbar (1열 넓게 편집 vs 3열 격자 보기 + 기본값 초기화)
        LinearLayout editModeBar = new LinearLayout(this);
        editModeBar.setOrientation(LinearLayout.HORIZONTAL);
        editModeBar.setGravity(Gravity.CENTER_VERTICAL);
        editModeBar.setPadding(dp(10), dp(8), dp(10), dp(8));
        editModeBar.setBackground(roundedBox(Color.parseColor("#FFFBEB"), Color.parseColor("#FDE68A"), 1, 10));

        TextView editGuideTv = new TextView(this);
        editGuideTv.setText("✏️ 각 번호의 [💬출력 글자]와 [🎭포즈 프롬프트]를 모두 직접 수정할 수 있습니다.");
        editGuideTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        editGuideTv.setTypeface(Typeface.DEFAULT_BOLD);
        editGuideTv.setTextColor(COLOR_KAKAO_BROWN);
        editModeBar.addView(editGuideTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView toggleColBtn = new TextView(this);
        toggleColBtn.setText("📋 1열 넓게 편집");
        toggleColBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        toggleColBtn.setTypeface(Typeface.DEFAULT_BOLD);
        toggleColBtn.setTextColor(COLOR_KAKAO_YELLOW);
        toggleColBtn.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 8));
        toggleColBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        toggleColBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSingleColumnPoseEditMode = !isSingleColumnPoseEditMode;
                toggleColBtn.setText(isSingleColumnPoseEditMode ? "🔢 3열 격자 보기" : "📋 1열 넓게 편집");
                rebuildPhrasesGridColumns();
            }
        });
        LinearLayout.LayoutParams tcbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tcbLp.leftMargin = dp(6);
        editModeBar.addView(toggleColBtn, tcbLp);

        TextView resetPosesBtn = new TextView(this);
        resetPosesBtn.setText("🔄 초기화");
        resetPosesBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        resetPosesBtn.setTypeface(Typeface.DEFAULT_BOLD);
        resetPosesBtn.setTextColor(COLOR_TEXT_MAIN);
        resetPosesBtn.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 8));
        resetPosesBtn.setPadding(dp(8), dp(6), dp(8), dp(6));
        resetPosesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetCurrentThemePhrasesAndPoses();
                Toast.makeText(MainActivity.this, "현재 테마의 기본 문구와 포즈 프롬프트로 초기화되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams rpbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rpbLp.leftMargin = dp(6);
        editModeBar.addView(resetPosesBtn, rpbLp);

        LinearLayout.LayoutParams embLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        embLp.bottomMargin = dp(8);
        langCard.addView(editModeBar, embLp);

        // 3x5 Editable Phrases & Poses Grid
        phrasesGrid = new GridLayout(this);
        phrasesGrid.setColumnCount(3);
        String[] defaultKo = LANGUAGE_PRESETS.get(0).defaultPhrases;
        for (int i = 0; i < 15; i++) {
            final int slotIndex = i;
            LinearLayout pCell = new LinearLayout(this);
            pCell.setOrientation(LinearLayout.VERTICAL);
            pCell.setPadding(dp(6), dp(6), dp(6), dp(6));
            pCell.setBackground(roundedBox(Color.parseColor("#F8FAFC"), COLOR_BORDER, 1, 8));

            int r = (i / 3) + 1;
            int c = (i % 3) + 1;

            LinearLayout cellHeadRow = new LinearLayout(this);
            cellHeadRow.setOrientation(LinearLayout.HORIZONTAL);
            cellHeadRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView badgeTv = new TextView(this);
            badgeTv.setText("#" + (i + 1) + " (" + r + "행 " + c + "열)");
            badgeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            badgeTv.setTextColor(COLOR_KAKAO_BROWN);
            badgeTv.setTypeface(Typeface.DEFAULT_BOLD);
            cellHeadRow.addView(badgeTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView expandEditTv = new TextView(this);
            expandEditTv.setText("🔍크게편집");
            expandEditTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            expandEditTv.setTextColor(Color.parseColor("#2563EB"));
            expandEditTv.setTypeface(Typeface.DEFAULT_BOLD);
            expandEditTv.setPadding(dp(4), dp(2), dp(4), dp(2));
            expandEditTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSingleStickerDetailEditorDialog(slotIndex);
                }
            });
            cellHeadRow.addView(expandEditTv);

            TextView phraseLbl = new TextView(this);
            phraseLbl.setText("💬 출력 글자:");
            phraseLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            phraseLbl.setTextColor(COLOR_TEXT_SUB);
            phraseLbl.setTypeface(Typeface.DEFAULT_BOLD);

            EditText pEdit = new EditText(this);
            pEdit.setText(defaultKo[i]);
            pEdit.setHint("출력할 글자 입력");
            pEdit.setSingleLine(true);
            pEdit.setGravity(Gravity.CENTER);
            pEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            pEdit.setTypeface(Typeface.DEFAULT_BOLD);
            pEdit.setPadding(dp(4), dp(4), dp(4), dp(4));
            pEdit.setBackground(roundedBox(Color.WHITE, Color.parseColor("#CBD5E1"), 1, 6));
            pEdit.addTextChangedListener(new SimpleWatcher() {
                @Override
                public void onChanged() {
                    if (!isUpdatingPhrasesProgrammatically) {
                        refreshPromptIfAuto();
                    }
                }
            });
            phraseInputs[i] = pEdit;

            TextView poseLbl = new TextView(this);
            poseLbl.setText("🎭 포즈/행동 프롬프트 (수정 가능):");
            poseLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            poseLbl.setTextColor(Color.parseColor("#1E40AF"));
            poseLbl.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams poseLblLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            poseLblLp.topMargin = dp(4);

            EditText sceneEdit = new EditText(this);
            sceneEdit.setText(DEFAULT_SCENES[i]);
            sceneEdit.setHint("이모티콘 포즈·표정·소품 프롬프트 입력");
            sceneEdit.setMinLines(2);
            sceneEdit.setMaxLines(4);
            sceneEdit.setGravity(Gravity.TOP | Gravity.START);
            sceneEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            sceneEdit.setTextColor(COLOR_TEXT_MAIN);
            sceneEdit.setPadding(dp(5), dp(4), dp(5), dp(4));
            sceneEdit.setBackground(roundedBox(Color.parseColor("#EFF6FF"), Color.parseColor("#93C5FD"), 1, 6));
            sceneEdit.addTextChangedListener(new SimpleWatcher() {
                @Override
                public void onChanged() {
                    if (!isUpdatingPhrasesProgrammatically) {
                        refreshPromptIfAuto();
                    }
                }
            });
            sceneInputs[i] = sceneEdit;

            pCell.addView(cellHeadRow);
            pCell.addView(phraseLbl);
            pCell.addView(pEdit);
            pCell.addView(poseLbl, poseLblLp);
            pCell.addView(sceneEdit);

            phraseCells[i] = pCell;
            GridLayout.LayoutParams pGlp = new GridLayout.LayoutParams();
            pGlp.width = 0;
            pGlp.columnSpec = GridLayout.spec(i % 3, 1, 1f);
            pGlp.setMargins(dp(3), dp(3), dp(3), dp(3));
            phrasesGrid.addView(pCell, pGlp);
        }
        langCard.addView(phrasesGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Auto-Generated Prompt Header + EditText
        LinearLayout promptToolbar = new LinearLayout(this);
        promptToolbar.setOrientation(LinearLayout.HORIZONTAL);
        promptToolbar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptLp.topMargin = dp(10);

        TextView promptLabel = new TextView(this);
        promptLabel.setText("✨ 생성 프롬프트 (자동 생성 · 직접 편집 가능)");
        promptLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        promptLabel.setTypeface(Typeface.DEFAULT_BOLD);
        promptLabel.setTextColor(COLOR_TEXT_MAIN);
        promptToolbar.addView(promptLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        autoPromptCheckBox = new CheckBox(this);
        autoPromptCheckBox.setText("자동 갱신");
        autoPromptCheckBox.setChecked(true);
        autoPromptCheckBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        promptToolbar.addView(autoPromptCheckBox);

        langCard.addView(promptToolbar, ptLp);

        promptEditText = new EditText(this);
        promptEditText.setMinLines(5);
        promptEditText.setMaxLines(8);
        promptEditText.setGravity(Gravity.TOP | Gravity.START);
        promptEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        promptEditText.setPadding(dp(10), dp(8), dp(10), dp(8));
        promptEditText.setBackground(roundedBox(Color.parseColor("#F8FAFC"), COLOR_BORDER, 1, 10));
        LinearLayout.LayoutParams peLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        peLp.topMargin = dp(4);
        langCard.addView(promptEditText, peLp);

        contentList.addView(langCard, cardMarginParams());

        // --- CARD 4: BACKGROUND MODE (WHITE vs TRANSPARENT), SIZE & EMAIL ---
        LinearLayout bgCard = createSectionCard("#FFFFFF", "#E5E8EB");
        bgCard.addView(createSectionHeader("4", "스티커 배경 선택 & 이메일 자동 발송", "정지형 PNG 전용 (GIF 제외)"));

        LinearLayout bgRow = new LinearLayout(this);
        bgRow.setOrientation(LinearLayout.HORIZONTAL);

        bgWhiteCard = createBgOptionCard("⬜ 순수 흰색 배경 (#FFFFFF)", "깔끔한 화이트 사각 스티커", false);
        bgWhiteCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedBgMode = "white";
                refreshBgOptionUi();
            }
        });
        LinearLayout.LayoutParams bwLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bwLp.rightMargin = dp(8);
        bgRow.addView(bgWhiteCard, bwLp);

        bgTransparentCard = createBgOptionCard("🏁 투명 배경 (Transparent PNG)", "외곽 흰 배경 제거 투명 PNG", true);
        bgTransparentCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedBgMode = "transparent";
                refreshBgOptionUi();
            }
        });
        bgRow.addView(bgTransparentCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bgCard.addView(bgRow);
        refreshBgOptionUi();

        // Size buttons
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.topMargin = dp(10);
        int[] sizes = new int[]{108, 180, 360};
        String[] sizeLabels = new String[]{"108×108 (30%)", "180×180 (중간)", "360×360 (원본)"};
        for (int i = 0; i < sizes.length; i++) {
            final int sz = sizes[i];
            TextView sBtn = new TextView(this);
            sBtn.setText(sizeLabels[i]);
            sBtn.setGravity(Gravity.CENTER);
            sBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
            sBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            sBtn.setTypeface(Typeface.DEFAULT_BOLD);
            sBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedStickerSize = sz;
                    refreshSizeBtnUi();
                }
            });
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < sizes.length - 1) sLp.rightMargin = dp(6);
            sizeRow.addView(sBtn, sLp);
            sizeBtnViews.put(sz, sBtn);
        }
        bgCard.addView(sizeRow, srLp);
        refreshSizeBtnUi();

        // Recipient Email
        TextView emailLabel = new TextView(this);
        emailLabel.setText("📧 완성물 수신 이메일 주소 (작성 완료 시 공유/이메일 창 자동 실행)");
        emailLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        emailLabel.setTypeface(Typeface.DEFAULT_BOLD);
        emailLabel.setTextColor(COLOR_TEXT_MAIN);
        LinearLayout.LayoutParams elLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elLp.topMargin = dp(12);
        bgCard.addView(emailLabel, elLp);

        emailInput = new EditText(this);
        emailInput.setText("jamesa75@gmail.com");
        emailInput.setHint("예: jamesa75@gmail.com (비워두어도 완료 후 공유 버튼으로 열기 가능)");
        emailInput.setSingleLine(true);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        emailInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        emailInput.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        LinearLayout.LayoutParams eiLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eiLp.topMargin = dp(4);
        bgCard.addView(emailInput, eiLp);

        contentList.addView(bgCard, cardMarginParams());

        // --- CARD 5: RESULTS PREVIEW, RE-OPEN SHARE SCREEN BUTTON, INSTANT BG SWITCH, ZIP & EMAIL ---
        resultSectionCard = createSectionCard("#FFFFFF", "#10B981");
        resultSectionCard.setVisibility(View.GONE);

        LinearLayout resHead = new LinearLayout(this);
        resHead.setOrientation(LinearLayout.HORIZONTAL);
        resHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView resTitle = new TextView(this);
        resTitle.setText("✅ 이모티콘 15종 자체 생성 완료");
        resTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        resTitle.setTypeface(Typeface.DEFAULT_BOLD);
        resTitle.setTextColor(COLOR_KAKAO_BROWN);
        resHead.addView(resTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        resultMetaBadge = new TextView(this);
        resultMetaBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        resultMetaBadge.setTypeface(Typeface.DEFAULT_BOLD);
        resultMetaBadge.setTextColor(COLOR_KAKAO_YELLOW);
        resultMetaBadge.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 99));
        resultMetaBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        resHead.addView(resultMetaBadge);
        resultSectionCard.addView(resHead);

        resultStatusBanner = new TextView(this);
        resultStatusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        resultStatusBanner.setPadding(dp(10), dp(8), dp(10), dp(8));
        resultStatusBanner.setBackground(roundedBox(Color.parseColor("#ECFDF5"), Color.parseColor("#A7F3D0"), 1, 8));
        resultStatusBanner.setTextColor(Color.parseColor("#065F46"));
        LinearLayout.LayoutParams rsbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rsbLp.topMargin = dp(8);
        resultSectionCard.addView(resultStatusBanner, rsbLp);

        // PROMINENT TOP BUTTON TO RE-OPEN THE COMPLETION SHARE / EMAIL SCREEN ANYTIME
        Button reopenShareTopBtn = new Button(this);
        reopenShareTopBtn.setText("📤 완성 후 나온 전체 공유 / 이메일 화면 다시 열기");
        reopenShareTopBtn.setAllCaps(false);
        reopenShareTopBtn.setTypeface(Typeface.DEFAULT_BOLD);
        reopenShareTopBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        reopenShareTopBtn.setTextColor(COLOR_KAKAO_BROWN);
        reopenShareTopBtn.setBackground(roundedBox(COLOR_KAKAO_YELLOW, COLOR_KAKAO_BROWN, 2, 12));
        reopenShareTopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reopenCompletionShareScreen();
            }
        });
        LinearLayout.LayoutParams rstLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        rstLp.topMargin = dp(10);
        resultSectionCard.addView(reopenShareTopBtn, rstLp);

        // Instant Offline BG Switch Buttons
        LinearLayout switchBgRow = new LinearLayout(this);
        switchBgRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sbrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbrLp.topMargin = dp(10);

        Button swWhiteBtn = new Button(this);
        swWhiteBtn.setText("⬜ 흰색 배경으로 즉시 변환");
        swWhiteBtn.setAllCaps(false);
        swWhiteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        swWhiteBtn.setBackground(roundedBox(Color.parseColor("#F1F5F9"), COLOR_BORDER, 1, 10));
        swWhiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reprocessBgOffline("white");
            }
        });
        LinearLayout.LayoutParams swwLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        swwLp.rightMargin = dp(6);
        switchBgRow.addView(swWhiteBtn, swwLp);

        Button swTransBtn = new Button(this);
        swTransBtn.setText("🏁 투명 배경으로 즉시 변환");
        swTransBtn.setAllCaps(false);
        swTransBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        swTransBtn.setBackground(roundedBox(Color.parseColor("#F1F5F9"), COLOR_BORDER, 1, 10));
        swTransBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reprocessBgOffline("transparent");
            }
        });
        switchBgRow.addView(swTransBtn, new LinearLayout.LayoutParams(0, dp(40), 1f));
        resultSectionCard.addView(switchBgRow, sbrLp);

        // 3x5 Sheet Preview
        sheetPreviewContainer = new FrameLayout(this);
        sheetPreviewContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams spcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(340));
        spcLp.topMargin = dp(10);
        resultSheetImageView = new ImageView(this);
        resultSheetImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sheetPreviewContainer.addView(resultSheetImageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        resultSectionCard.addView(sheetPreviewContainer, spcLp);

        // 15 Individual Stickers Grid
        resultStickersGrid = new GridLayout(this);
        resultStickersGrid.setColumnCount(3);
        LinearLayout.LayoutParams rsgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rsgLp.topMargin = dp(10);
        resultSectionCard.addView(resultStickersGrid, rsgLp);

        // Save ZIP Button
        Button saveZipBtn = new Button(this);
        saveZipBtn.setText("📦 전체 시트 + 개별 15종 ZIP 다운로드 폴더 저장");
        saveZipBtn.setAllCaps(false);
        saveZipBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveZipBtn.setTextColor(Color.WHITE);
        saveZipBtn.setBackground(roundedBox(Color.parseColor("#10B981"), 0, 0, 12));
        saveZipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastSliceResult != null) {
                    try {
                        String zName = "kakao_stickers_15pack_" + lastSliceResult.bgMode + "_" + lastSliceResult.stickerSize + "px.zip";
                        writeBytesToDownloads(lastSliceResult.zipBytes, "application/zip", zName);
                        Toast.makeText(MainActivity.this, "📦 저장 완료: Downloads/KakaoStickers/" + zName, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        LinearLayout.LayoutParams szbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        szbLp.topMargin = dp(12);
        resultSectionCard.addView(saveZipBtn, szbLp);

        // Bottom Re-open Share/Email Row
        LinearLayout resendRow = new LinearLayout(this);
        resendRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rrLp.topMargin = dp(8);

        resendEmailInput = new EditText(this);
        resendEmailInput.setHint("수신 이메일 주소");
        resendEmailInput.setSingleLine(true);
        resendEmailInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        resendEmailInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        resendEmailInput.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        LinearLayout.LayoutParams reiLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        reiLp.rightMargin = dp(6);
        resendRow.addView(resendEmailInput, reiLp);

        Button resendBtn = new Button(this);
        resendBtn.setText("📤 공유/이메일 화면 다시 열기");
        resendBtn.setAllCaps(false);
        resendBtn.setTypeface(Typeface.DEFAULT_BOLD);
        resendBtn.setTextColor(COLOR_KAKAO_YELLOW);
        resendBtn.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 10));
        resendBtn.setPadding(dp(12), 0, dp(12), 0);
        resendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reopenCompletionShareScreen();
            }
        });
        resendRow.addView(resendBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));
        resultSectionCard.addView(resendRow, rrLp);

        contentList.addView(resultSectionCard, cardMarginParams());

        mainScrollView.addView(contentList);
        rootScreen.addView(mainScrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 3. Bottom Sticky Action Button Bar (with persistent "📤 공유 화면 다시 열기" button once generated)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.WHITE);
        bottomBar.setPadding(dp(14), dp(10), dp(14), dp(10));

        stickyReopenShareBtn = new Button(this);
        stickyReopenShareBtn.setText("📤 공유 화면 다시 열기");
        stickyReopenShareBtn.setAllCaps(false);
        stickyReopenShareBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        stickyReopenShareBtn.setTypeface(Typeface.DEFAULT_BOLD);
        stickyReopenShareBtn.setTextColor(COLOR_KAKAO_YELLOW);
        stickyReopenShareBtn.setBackground(roundedBox(COLOR_KAKAO_BROWN, 0, 0, 14));
        stickyReopenShareBtn.setVisibility(View.GONE);
        stickyReopenShareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reopenCompletionShareScreen();
            }
        });
        LinearLayout.LayoutParams srsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(52));
        srsLp.rightMargin = dp(8);
        stickyReopenShareBtn.setPadding(dp(14), 0, dp(14), 0);
        bottomBar.addView(stickyReopenShareBtn, srsLp);

        generateBtn = new Button(this);
        generateBtn.setText("🚀 이모티콘 15종 앱 자체 생성 시작");
        generateBtn.setAllCaps(false);
        generateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        generateBtn.setTypeface(Typeface.DEFAULT_BOLD);
        generateBtn.setTextColor(COLOR_KAKAO_BROWN);
        generateBtn.setBackground(roundedBox(COLOR_KAKAO_YELLOW, COLOR_KAKAO_BROWN, 1, 14));
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOnDeviceGeneration();
            }
        });
        bottomBar.addView(generateBtn, new LinearLayout.LayoutParams(
                0, dp(52), 1f));

        rootScreen.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(rootScreen);
    }

    private void reopenCompletionShareScreen() {
        if (lastSliceResult == null) {
            Toast.makeText(this, "먼저 이모티콘 15종을 생성해 주세요!", Toast.LENGTH_SHORT).show();
            return;
        }
        String targetEmail = "";
        if (resendEmailInput != null && !resendEmailInput.getText().toString().trim().isEmpty()) {
            targetEmail = resendEmailInput.getText().toString().trim();
        } else if (emailInput != null) {
            targetEmail = emailInput.getText().toString().trim();
        }
        stageFilesForEmailAndLaunchIntent(lastSliceResult, targetEmail);
    }

    private void selectThemeMode(String themeId) {
        selectedThemeId = themeId;
        refreshThemeTabsUi();
        if (fishingSpeciesContainer != null) {
            fishingSpeciesContainer.setVisibility("fishing".equals(themeId) ? View.VISIBLE : View.GONE);
        }
        resetCurrentThemePhrasesAndPoses();
    }

    private String[] getCurrentSceneCompositions() {
        if ("fishing".equals(selectedThemeId)) return FISHING_SCENES;
        if ("travel".equals(selectedThemeId)) return TRAVEL_SCENES;
        return DEFAULT_SCENES;
    }

    private void resetCurrentThemePhrasesAndPoses() {
        LanguagePreset lp = getSelectedLanguagePreset();
        String[] targetPhrases = lp.getPhrasesForTheme(selectedThemeId);
        String[] targetScenes = getCurrentSceneCompositions();

        isUpdatingPhrasesProgrammatically = true;
        for (int i = 0; i < 15; i++) {
            phraseInputs[i].setText(targetPhrases[i]);
            if (sceneInputs[i] != null) {
                sceneInputs[i].setText(targetScenes[i]);
            }
        }
        isUpdatingPhrasesProgrammatically = false;

        if ("fishing".equals(selectedThemeId) && !FISHING_SPECIES[0].equals(selectedFishTarget)) {
            applySingleFishFocusIfSelected(selectedFishTarget);
        }
        refreshPromptIfAuto();
    }

    private void applyCurrentThemeAndLanguagePhrases() {
        LanguagePreset lp = getSelectedLanguagePreset();
        String[] targetPhrases = lp.getPhrasesForTheme(selectedThemeId);
        String[] targetScenes = getCurrentSceneCompositions();

        isUpdatingPhrasesProgrammatically = true;
        for (int i = 0; i < 15; i++) {
            phraseInputs[i].setText(targetPhrases[i]);
            if (sceneInputs[i] != null && sceneInputs[i].getText().toString().trim().isEmpty()) {
                sceneInputs[i].setText(targetScenes[i]);
            }
        }
        isUpdatingPhrasesProgrammatically = false;

        if ("fishing".equals(selectedThemeId) && !FISHING_SPECIES[0].equals(selectedFishTarget)) {
            applySingleFishFocusIfSelected(selectedFishTarget);
        }
        refreshPromptIfAuto();
    }

    private void applySingleFishFocusIfSelected(String fishName) {
        if (!"fishing".equals(selectedThemeId)) return;
        if (fishName == null || fishName.equals(FISHING_SPECIES[0])) {
            // Reset to all 6 fish species across the 15 stickers
            LanguagePreset lp = getSelectedLanguagePreset();
            String[] basePhrases = lp.fishingPhrases;
            isUpdatingPhrasesProgrammatically = true;
            for (int i = 0; i < 15; i++) {
                phraseInputs[i].setText(basePhrases[i]);
                if (sceneInputs[i] != null) {
                    sceneInputs[i].setText(FISHING_SCENES[i]);
                }
            }
            isUpdatingPhrasesProgrammatically = false;
            return;
        }
        // Customize catch slots (#4~#9) for both phrases AND pose prompts for the selected target fish
        isUpdatingPhrasesProgrammatically = true;
        if ("ko".equals(selectedLanguageId)) {
            phraseInputs[3].setText("대물 " + fishName + " 잡았다!");
            phraseInputs[4].setText(fishName + " 손맛 최고!");
            phraseInputs[5].setText("인생 " + fishName + " 인증!");
            phraseInputs[6].setText(fishName + " 대박 히트!");
            phraseInputs[7].setText("특대 " + fishName + " 등판!");
            phraseInputs[8].setText("몬스터 " + fishName + " 제압!");
        }
        if (sceneInputs[3] != null) {
            sceneInputs[3].setText("잡아 올린 아주 큰 대물 " + fishName + "를 두 손으로 번쩍 들고 카메라를 보며 자랑스럽게 인증 사진 포즈 잡는 모습");
        }
        if (sceneInputs[4] != null) {
            sceneInputs[4].setText("큼직하고 싱싱한 대물 " + fishName + "를 품에 안고 환호하며 카메라를 향해 인증 사진 포즈 잡는 모습");
        }
        if (sceneInputs[5] != null) {
            sceneInputs[5].setText("역대급 크기의 대물 " + fishName + "를 한 손에 높이 들고 엄지척하며 당당하게 사진 포즈 잡는 모습");
        }
        if (sceneInputs[6] != null) {
            sceneInputs[6].setText("반짝이는 대물 " + fishName + "를 높이 들어 올리며 신나게 인증 사진 포즈 잡는 모습");
        }
        if (sceneInputs[7] != null) {
            sceneInputs[7].setText("특대 사이즈 " + fishName + "를 들어 올리며 깜짝 놀라고 기뻐하며 인증 사진 포즈 잡는 모습");
        }
        if (sceneInputs[8] != null) {
            sceneInputs[8].setText("몸집만 한 거대한 몬스터 " + fishName + "를 두 팔로 힘껏 안아 들고 위풍당당하게 사진 포즈 잡는 모습");
        }
        isUpdatingPhrasesProgrammatically = false;
    }

    private void rebuildPhrasesGridColumns() {
        if (phrasesGrid == null) return;
        phrasesGrid.removeAllViews();
        int cols = isSingleColumnPoseEditMode ? 1 : 3;
        phrasesGrid.setColumnCount(cols);
        for (int i = 0; i < 15; i++) {
            LinearLayout cell = phraseCells[i];
            if (cell == null) continue;
            GridLayout.LayoutParams pGlp = new GridLayout.LayoutParams();
            pGlp.width = 0;
            pGlp.columnSpec = GridLayout.spec(i % cols, 1, 1f);
            pGlp.setMargins(dp(3), dp(4), dp(3), dp(4));
            phrasesGrid.addView(cell, pGlp);
        }
    }

    private void openSingleStickerDetailEditorDialog(final int index) {
        int r = (index / 3) + 1;
        int c = (index % 3) + 1;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(12));

        TextView pTitle = new TextView(this);
        pTitle.setText("💬 이모티콘 출력 글자 (캐릭터 머리 위 텍스트)");
        pTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        pTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pTitle.setTextColor(COLOR_TEXT_MAIN);
        box.addView(pTitle);

        final EditText dlgPhraseEdit = new EditText(this);
        dlgPhraseEdit.setText(phraseInputs[index].getText().toString());
        dlgPhraseEdit.setSingleLine(true);
        dlgPhraseEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        dlgPhraseEdit.setTypeface(Typeface.DEFAULT_BOLD);
        dlgPhraseEdit.setPadding(dp(12), dp(10), dp(12), dp(10));
        dlgPhraseEdit.setBackground(roundedBox(Color.WHITE, Color.parseColor("#CBD5E1"), 1, 10));
        LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpLp.topMargin = dp(6);
        dpLp.bottomMargin = dp(14);
        box.addView(dlgPhraseEdit, dpLp);

        TextView sTitle = new TextView(this);
        sTitle.setText("🎭 이모티콘 포즈 / 표정 / 상황 프롬프트");
        sTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sTitle.setTextColor(Color.parseColor("#1E40AF"));
        box.addView(sTitle);

        final EditText dlgSceneEdit = new EditText(this);
        dlgSceneEdit.setText(sceneInputs[index].getText().toString());
        dlgSceneEdit.setMinLines(3);
        dlgSceneEdit.setMaxLines(6);
        dlgSceneEdit.setGravity(Gravity.TOP | Gravity.START);
        dlgSceneEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        dlgSceneEdit.setPadding(dp(12), dp(10), dp(12), dp(10));
        dlgSceneEdit.setBackground(roundedBox(Color.parseColor("#EFF6FF"), Color.parseColor("#93C5FD"), 1, 10));
        LinearLayout.LayoutParams dsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dsLp.topMargin = dp(6);
        box.addView(dlgSceneEdit, dsLp);

        new AlertDialog.Builder(this)
                .setTitle("#" + (index + 1) + " (" + r + "행 " + c + "열) 문구 & 포즈 편집")
                .setView(box)
                .setPositiveButton("적용", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        phraseInputs[index].setText(dlgPhraseEdit.getText().toString().trim());
                        sceneInputs[index].setText(dlgSceneEdit.getText().toString().trim());
                        refreshPromptIfAuto();
                    }
                })
                .setNeutralButton("기본값 복원", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String[] defP = getSelectedLanguagePreset().getPhrasesForTheme(selectedThemeId);
                        String[] defS = getCurrentSceneCompositions();
                        phraseInputs[index].setText(defP[index]);
                        sceneInputs[index].setText(defS[index]);
                        refreshPromptIfAuto();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private LinearLayout.LayoutParams cardMarginParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        return lp;
    }

    private LinearLayout createSectionCard(String bgHex, String strokeHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(roundedBox(Color.parseColor(bgHex), Color.parseColor(strokeHex), 1, 16));
        return card;
    }

    private View createSectionHeader(String stepText, String title, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);

        TextView stepTv = new TextView(this);
        stepTv.setText(stepText);
        stepTv.setGravity(Gravity.CENTER);
        stepTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        stepTv.setTypeface(Typeface.DEFAULT_BOLD);
        stepTv.setTextColor(COLOR_KAKAO_BROWN);
        stepTv.setBackground(roundedBox(COLOR_KAKAO_YELLOW, 0, 0, 99));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        sLp.rightMargin = dp(6);
        row.addView(stepTv, sLp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(COLOR_KAKAO_BROWN);
        row.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView hintTv = new TextView(this);
        hintTv.setText(hint);
        hintTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hintTv.setTextColor(COLOR_TEXT_SUB);
        row.addView(hintTv);
        return row;
    }

    private LinearLayout createBgOptionCard(String title, String sub, boolean checker) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView preview = new TextView(this);
        preview.setText(checker ? "Alpha 투명 배경" : "#FFFFFF 흰색 배경");
        preview.setGravity(Gravity.CENTER);
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        preview.setTypeface(Typeface.DEFAULT_BOLD);
        preview.setTextColor(COLOR_TEXT_MAIN);
        if (checker) {
            preview.setBackground(createCheckerboardDrawable());
        } else {
            preview.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 6));
        }
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32));
        pLp.bottomMargin = dp(6);
        box.addView(preview, pLp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(COLOR_TEXT_MAIN);

        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        s.setTextColor(COLOR_TEXT_SUB);

        box.addView(t);
        box.addView(s);
        return box;
    }

    private void showApiKeyStatus(boolean ok, String msg) {
        apiKeyStatusView.setVisibility(View.VISIBLE);
        apiKeyStatusView.setText(msg);
        if (ok) {
            apiKeyStatusView.setBackground(roundedBox(Color.parseColor("#ECFDF5"), Color.parseColor("#A7F3D0"), 1, 8));
            apiKeyStatusView.setTextColor(Color.parseColor("#065F46"));
        } else {
            apiKeyStatusView.setBackground(roundedBox(Color.parseColor("#FEF2F2"), Color.parseColor("#FECACA"), 1, 8));
            apiKeyStatusView.setTextColor(Color.parseColor("#991B1B"));
        }
    }

    private void refreshMoodSelectionUi() {
        for (Map.Entry<String, LinearLayout> entry : moodCardViews.entrySet()) {
            boolean active = entry.getKey().equals(selectedMoodId);
            entry.getValue().setBackground(roundedBox(
                    active ? Color.parseColor("#FFFDF0") : Color.WHITE,
                    active ? COLOR_KAKAO_BROWN : COLOR_BORDER,
                    active ? 2 : 1,
                    12));
        }
    }

    private void refreshThemeTabsUi() {
        for (Map.Entry<String, TextView> entry : themeTabViews.entrySet()) {
            boolean active = entry.getKey().equals(selectedThemeId);
            TextView tv = entry.getValue();
            tv.setTextColor(active ? COLOR_KAKAO_BROWN : COLOR_TEXT_SUB);
            tv.setBackground(roundedBox(
                    active ? COLOR_KAKAO_YELLOW : Color.parseColor("#F8FAFC"),
                    active ? COLOR_KAKAO_BROWN : COLOR_BORDER,
                    active ? 2 : 1,
                    10));
        }
    }

    private void refreshFishingSpeciesChipsUi() {
        for (Map.Entry<String, TextView> entry : fishingSpeciesChipViews.entrySet()) {
            boolean active = entry.getKey().equals(selectedFishTarget);
            TextView tv = entry.getValue();
            tv.setTextColor(active ? Color.WHITE : Color.parseColor("#1E3A8A"));
            tv.setBackground(roundedBox(
                    active ? Color.parseColor("#1D4ED8") : Color.WHITE,
                    active ? Color.parseColor("#1E3A8A") : Color.parseColor("#93C5FD"),
                    1,
                    99));
        }
    }

    private void refreshLanguageTabsUi() {
        for (Map.Entry<String, TextView> entry : langTabViews.entrySet()) {
            boolean active = entry.getKey().equals(selectedLanguageId);
            TextView tv = entry.getValue();
            tv.setTextColor(active ? COLOR_KAKAO_YELLOW : COLOR_TEXT_SUB);
            tv.setBackground(roundedBox(
                    active ? COLOR_KAKAO_BROWN : Color.WHITE,
                    active ? COLOR_KAKAO_BROWN : COLOR_BORDER,
                    1,
                    99));
        }
    }

    private void refreshBgOptionUi() {
        boolean whiteActive = "white".equals(selectedBgMode);
        bgWhiteCard.setBackground(roundedBox(
                whiteActive ? Color.parseColor("#FFFDF0") : Color.WHITE,
                whiteActive ? COLOR_KAKAO_BROWN : COLOR_BORDER,
                whiteActive ? 2 : 1,
                12));
        bgTransparentCard.setBackground(roundedBox(
                !whiteActive ? Color.parseColor("#FFFDF0") : Color.WHITE,
                !whiteActive ? COLOR_KAKAO_BROWN : COLOR_BORDER,
                !whiteActive ? 2 : 1,
                12));
    }

    private void refreshSizeBtnUi() {
        for (Map.Entry<Integer, TextView> entry : sizeBtnViews.entrySet()) {
            boolean active = entry.getKey() == selectedStickerSize;
            TextView tv = entry.getValue();
            tv.setTextColor(active ? COLOR_KAKAO_YELLOW : COLOR_TEXT_MAIN);
            tv.setBackground(roundedBox(
                    active ? COLOR_KAKAO_BROWN : Color.WHITE,
                    active ? COLOR_KAKAO_BROWN : COLOR_BORDER,
                    1,
                    8));
        }
    }

    private List<String> getCurrentPhrasesList() {
        List<String> list = new ArrayList<>(15);
        for (int i = 0; i < 15; i++) {
            list.add(phraseInputs[i].getText().toString().trim());
        }
        return list;
    }

    private List<String> getCurrentScenesList() {
        List<String> list = new ArrayList<>(15);
        String[] fallback = getCurrentSceneCompositions();
        for (int i = 0; i < 15; i++) {
            String val = sceneInputs[i] != null ? sceneInputs[i].getText().toString().trim() : "";
            list.add(val.isEmpty() ? fallback[i] : val);
        }
        return list;
    }

    private MoodPreset getSelectedMoodPreset() {
        for (MoodPreset m : MOOD_PRESETS) {
            if (m.id.equals(selectedMoodId)) return m;
        }
        return MOOD_PRESETS.get(0);
    }

    private LanguagePreset getSelectedLanguagePreset() {
        for (LanguagePreset l : LANGUAGE_PRESETS) {
            if (l.id.equals(selectedLanguageId)) return l;
        }
        return LANGUAGE_PRESETS.get(0);
    }

    private void refreshPromptIfAuto() {
        if (autoPromptCheckBox != null && autoPromptCheckBox.isChecked()) {
            promptEditText.setText(buildStickerPromptLocally());
        }
    }

    private String buildStickerPromptLocally() {
        MoodPreset mood = getSelectedMoodPreset();
        LanguagePreset lang = getSelectedLanguagePreset();
        List<String> phrases = getCurrentPhrasesList();
        List<String> scenes = getCurrentScenesList();
        String[] fallbackPhrases = lang.getPhrasesForTheme(selectedThemeId);

        String moodDesc = mood.stylePrompt;
        if ("fishing".equals(selectedThemeId)) {
            moodDesc += "\n[낚시 테마 특별 지시사항]: 낚시하는 역동적인 모습(캐스팅, 입질 대기, 챔질 히트, 뜰채질)과 큰 생선(광어, 참돔, 돌돔, 갈치, 문어, 부리 등)을 잡았을 때 두 손으로 자랑스럽게 들고 사진 포즈(인증샷 포즈)를 잡는 모습을 생생하고 귀엽게 표현해줘.";
            if (!FISHING_SPECIES[0].equals(selectedFishTarget)) {
                moodDesc += " 특히 대상 어종인 '" + selectedFishTarget + "'의 특징(체형, 무늬, 빛깔)이 한눈에 알아볼 수 있게 큼직하고 디테일하게 그려줘.";
            }
        } else if ("travel".equals(selectedThemeId)) {
            moodDesc += "\n[여행 테마 특별 지시사항]: 설레는 여행 출발, 비행기 탑승, 유명 명소 인증샷 사진 포즈, 현지 맛집 탐방, 휴양지 힐링 등 여행의 다채로운 순간과 소품(캐리어, 여권, 카메라, 선글라스)을 생생하게 표현해줘.";
        }

        String customMood = customMoodInput != null ? customMoodInput.getText().toString().trim() : "";
        if (!customMood.isEmpty()) {
            moodDesc += "\n추가 분위기 요청: " + customMood;
        }

        String[] rowNames = new String[]{"1행(첫째 줄 3개)", "2행(둘째 줄 3개)", "3행(셋째 줄 3개)", "4행(넷째 줄 3개)", "5행(다섯째 줄 3개)"};
        StringBuilder rowsSb = new StringBuilder();
        StringBuilder quotedSb = new StringBuilder();

        for (int r = 0; r < 5; r++) {
            rowsSb.append("- ").append(rowNames[r]).append(": ");
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                String pTxt = phrases.get(idx);
                if (pTxt.isEmpty()) pTxt = fallbackPhrases[idx];
                String sTxt = scenes.get(idx);
                if (idx > 0) quotedSb.append(", ");
                quotedSb.append("“").append(pTxt).append("”");
                if (c > 0) rowsSb.append(" | ");
                rowsSb.append("[").append(c + 1).append("열] ").append(pTxt).append(" (").append(sTxt).append(")");
            }
            if (r < 4) rowsSb.append("\n");
        }

        return "첨부한 사진 속 인물을 참고해 " + mood.label + " 분위기의 2D 카카오톡 스타일 이모티콘 시트를 제작해줘.\n"
                + moodDesc + "\n\n"
                + "[필수 레이아웃 규칙 - 절대 엄수]\n"
                + "1. 배경은 순수 흰색(#FFFFFF)으로 하고, 격자 선 없이 충분한 흰색 여백을 두어 미니멀한 메신저 스티커 미리보기 레이아웃으로 구성해줘.\n"
                + "2. 반드시 가로 3열(한 줄에 정확히 3개) x 세로 5행(총 5줄) = 정확히 15개의 독립된 이모티콘만 균등한 간격과 동일한 여백으로 배치해줘. (절대 한 줄에 4개를 넣거나 문구를 중복해서 넣지 말 것!)\n"
                + "3. 각 이모티콘의 문구는 반드시 캐릭터 머리 위쪽(상단 중앙)에 배치해줘.\n"
                + "4. " + lang.fontInstruction + "\n"
                + "5. 캐릭터는 사진 속 인물의 헤어스타일, 안경/선글라스 유무, 얼굴 느낌, 의상 분위기를 살린 동글동글한 SD 캐릭터로 표현하고, 15개 모두 같은 인물처럼 보이도록 얼굴형, 머리색, 헤어스타일, 피부톤, 그림체를 통일해줘.\n"
                + "6. 표정과 손동작은 크고 과장되게 표현하고, 하트·별·눈물·땀방울·충격선·불꽃·폭죽 같은 귀여운 효과를 넣어 감정이 한눈에 보이게 해줘.\n\n"
                + "문구는 다음 15개를 각각 하나씩 넣어줘:\n" + quotedSb + "\n\n"
                + "[3열 x 5행 문구 및 표준 상황·소품 구성 (중복 없이 정확히 아래 순서대로 배치)]\n"
                + rowsSb + "\n\n"
                + "각 문구에 맞는 감정, 제스처, 상황을 다르게 표현하고, 전체적으로 밝고 통일감 있는 고품질 카카오톡 이모티콘 시트 느낌으로 완성해줘.";
    }

    private void loadSavedSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedKey = prefs.getString(KEY_GEMINI_API_KEY, "");
        String savedModel = prefs.getString(KEY_PREFERRED_MODEL, MODEL_IDS[0]);
        if (!savedKey.isEmpty()) {
            apiKeyInput.setText(savedKey);
        }
        for (int i = 0; i < MODEL_IDS.length; i++) {
            if (MODEL_IDS[i].equals(savedModel)) {
                modelSpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveCurrentSettings() {
        String key = apiKeyInput.getText().toString().trim();
        int idx = modelSpinner.getSelectedItemPosition();
        String modelId = (idx >= 0 && idx < MODEL_IDS.length) ? MODEL_IDS[idx] : MODEL_IDS[0];
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_GEMINI_API_KEY, key)
                .putString(KEY_PREFERRED_MODEL, modelId)
                .apply();
    }

    private void showProgressModal(String title, String desc) {
        if (progressDialog == null) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setPadding(dp(24), dp(24), dp(24), dp(24));
            box.setBackground(roundedBox(Color.WHITE, 0, 0, 16));

            ProgressBar pb = new ProgressBar(this);
            box.addView(pb, new LinearLayout.LayoutParams(dp(48), dp(48)));

            progressTitleView = new TextView(this);
            progressTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
            progressTitleView.setTypeface(Typeface.DEFAULT_BOLD);
            progressTitleView.setTextColor(COLOR_TEXT_MAIN);
            progressTitleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ptLp.topMargin = dp(12);
            box.addView(progressTitleView, ptLp);

            progressDescView = new TextView(this);
            progressDescView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            progressDescView.setTextColor(COLOR_TEXT_SUB);
            progressDescView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams pdLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pdLp.topMargin = dp(6);
            box.addView(progressDescView, pdLp);

            progressDialog = new AlertDialog.Builder(this)
                    .setView(box)
                    .setCancelable(false)
                    .create();
        }
        progressTitleView.setText(title);
        progressDescView.setText(desc);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void updateProgressModal(final String title, final String desc) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showProgressModal(title, desc);
            }
        });
    }

    private void hideProgressModal() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void startOnDeviceGeneration() {
        final String apiKey = apiKeyInput.getText().toString().trim();
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "먼저 상단 [🔑 Gemini API 키 설정]에 API 키를 입력해 주세요!", Toast.LENGTH_LONG).show();
            mainScrollView.smoothScrollTo(0, 0);
            apiKeyInput.requestFocus();
            return;
        }
        if (selectedPhotoBytes == null || selectedPhotoBytes.length == 0) {
            Toast.makeText(this, "먼저 Step 1에서 인물 사진을 선택하거나 카메라로 촬영해 주세요!", Toast.LENGTH_LONG).show();
            return;
        }

        saveCurrentSettings();
        final int modelIdx = modelSpinner.getSelectedItemPosition();
        final String preferredModel = (modelIdx >= 0 && modelIdx < MODEL_IDS.length) ? MODEL_IDS[modelIdx] : MODEL_IDS[0];
        final List<String> phrases = getCurrentPhrasesList();
        final String prompt = promptEditText.getText().toString().trim().isEmpty()
                ? buildStickerPromptLocally()
                : promptEditText.getText().toString().trim();
        final String bgMode = selectedBgMode;
        final int stickerSize = selectedStickerSize;
        final String recipientEmail = emailInput.getText().toString().trim();
        final String moodLabel = getSelectedMoodPreset().label;
        final String langLabel = getSelectedLanguagePreset().label;

        generateBtn.setEnabled(false);
        showProgressModal("1/4단계: 인물 중앙 크롭 중...", "스마트폰 내부에서 인물 중심 비율로 사진을 크롭하고 있습니다.");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap step1Bmp = OnDeviceStickerEngine.cropPersonCenter(selectedPhotoBytes, 1024);

                    updateProgressModal("2/4단계: 흰색 배경 분리 중...",
                            "사용자의 Gemini API 키로 인물 외 배경을 순수 흰색(#FFFFFF)으로 분리하고 있습니다.");
                    Bitmap step2Bmp = OnDeviceStickerEngine.replaceBackgroundWithWhite(apiKey, step1Bmp);

                    updateProgressModal("3/4단계: 3×5 이모티콘 시트 생성 중...",
                            "선택하신 분위기(" + moodLabel + ")와 15개 문구로 2D SD 이모티콘 시트를 생성하고 있습니다 (약 20~40초 소요).");
                    OnDeviceStickerEngine.PairResult sheetPair = OnDeviceStickerEngine.generateStickerSheet(
                            apiKey, step2Bmp, prompt, preferredModel);

                    updateProgressModal("4/4단계: 15종 자동 슬라이싱 및 배경 처리 중...",
                            "스마트폰 내부에서 15개 스티커 분할, " + ("transparent".equals(bgMode) ? "투명 배경(PNG)" : "흰색 배경") + " 처리 및 ZIP 압축을 수행 중입니다.");
                    final OnDeviceStickerEngine.SliceResult sr = OnDeviceStickerEngine.sliceStickerSheet(
                            sheetPair.bitmap, phrases, bgMode, stickerSize, sheetPair.model);

                    lastRawSheetBitmap = sheetPair.bitmap;
                    lastPhrases = phrases;
                    lastMoodLabel = moodLabel;
                    lastLanguageLabel = langLabel;
                    lastUsedModel = sheetPair.model;
                    lastSliceResult = sr;

                    final String zipName = "kakao_stickers_15pack_" + sr.bgMode + "_" + sr.stickerSize + "px.zip";
                    try {
                        writeBytesToDownloads(sr.zipBytes, "application/zip", zipName);
                    } catch (Exception ignored) {
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressModal();
                            generateBtn.setEnabled(true);
                            renderResultSectionUi(sr, zipName);
                            // Automatically launch the Share / Email screen upon completion
                            stageFilesForEmailAndLaunchIntent(sr, recipientEmail);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressModal();
                            generateBtn.setEnabled(true);
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("생성 실패")
                                    .setMessage(e.getMessage())
                                    .setPositiveButton("확인", null)
                                    .show();
                        }
                    });
                }
            }
        }).start();
    }

    private void reprocessBgOffline(final String newBgMode) {
        if (lastRawSheetBitmap == null) {
            Toast.makeText(this, "먼저 이모티콘을 생성해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedBgMode = newBgMode;
        refreshBgOptionUi();
        showProgressModal("배경 즉시 변환 중...", "API 재호출 없이 기기 내부에서 배경을 변환하고 있습니다.");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final OnDeviceStickerEngine.SliceResult sr = OnDeviceStickerEngine.sliceStickerSheet(
                            lastRawSheetBitmap, lastPhrases, newBgMode, selectedStickerSize, lastUsedModel);
                    lastSliceResult = sr;
                    final String zipName = "kakao_stickers_15pack_" + sr.bgMode + "_" + sr.stickerSize + "px.zip";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressModal();
                            renderResultSectionUi(sr, zipName);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressModal();
                            Toast.makeText(MainActivity.this, "배경 전환 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void renderResultSectionUi(final OnDeviceStickerEngine.SliceResult sr, String savedZipName) {
        resultSectionCard.setVisibility(View.VISIBLE);
        if (stickyReopenShareBtn != null) {
            stickyReopenShareBtn.setVisibility(View.VISIBLE);
        }
        boolean isTrans = "transparent".equalsIgnoreCase(sr.bgMode);
        resultMetaBadge.setText((isTrans ? "투명 배경 PNG" : "흰색 배경") + " · " + sr.stickerSize + "×" + sr.stickerSize + "px");
        resultStatusBanner.setText("✅ 기기 내 Downloads/KakaoStickers/" + savedZipName + " 자동 저장 완료 (모델: " + sr.usedModel + ")");

        if (isTrans) {
            sheetPreviewContainer.setBackground(createCheckerboardDrawable());
        } else {
            sheetPreviewContainer.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));
        }
        Bitmap dispBmp = BitmapFactory.decodeByteArray(sr.displaySheetPngBytes, 0, sr.displaySheetPngBytes.length);
        resultSheetImageView.setImageBitmap(dispBmp);

        resultStickersGrid.removeAllViews();
        for (int i = 0; i < sr.stickers.size(); i++) {
            final OnDeviceStickerEngine.SlicedSticker st = sr.stickers.get(i);
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(6), dp(6), dp(6), dp(6));
            cell.setBackground(roundedBox(Color.WHITE, COLOR_BORDER, 1, 10));

            FrameLayout imgWrap = new FrameLayout(this);
            if (isTrans) {
                imgWrap.setBackground(createCheckerboardDrawable());
            } else {
                imgWrap.setBackground(roundedBox(Color.WHITE, Color.parseColor("#F1F5F9"), 1, 8));
            }
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Bitmap sBmp = BitmapFactory.decodeByteArray(st.pngBytes, 0, st.pngBytes.length);
            iv.setImageBitmap(sBmp);
            imgWrap.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            cell.addView(imgWrap, new LinearLayout.LayoutParams(dp(86), dp(86)));

            TextView pTv = new TextView(this);
            pTv.setText(st.phrase);
            pTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            pTv.setTypeface(Typeface.DEFAULT_BOLD);
            pTv.setTextColor(COLOR_TEXT_MAIN);
            pTv.setSingleLine(true);
            pTv.setGravity(Gravity.CENTER);
            cell.addView(pTv);

            LinearLayout actRow = new LinearLayout(this);
            actRow.setOrientation(LinearLayout.HORIZONTAL);
            actRow.setGravity(Gravity.CENTER);

            TextView saveTv = new TextView(this);
            saveTv.setText("💾저장");
            saveTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            saveTv.setTextColor(Color.parseColor("#2563EB"));
            saveTv.setTypeface(Typeface.DEFAULT_BOLD);
            saveTv.setPadding(dp(4), dp(2), dp(6), dp(2));
            saveTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        writeBytesToDownloads(st.pngBytes, "image/png", st.filename);
                        Toast.makeText(MainActivity.this, "저장 완료: Downloads/KakaoStickers/" + st.filename, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "저장 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            TextView shareTv = new TextView(this);
            shareTv.setText("📤공유");
            shareTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            shareTv.setTextColor(COLOR_KAKAO_BROWN);
            shareTv.setTypeface(Typeface.DEFAULT_BOLD);
            shareTv.setPadding(dp(6), dp(2), dp(4), dp(2));
            shareTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareSingleStickerBytes(st.pngBytes, st.filename, st.phrase);
                }
            });

            actRow.addView(saveTv);
            actRow.addView(shareTv);
            cell.addView(actRow);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.columnSpec = GridLayout.spec(i % 3, 1, 1f);
            glp.setMargins(dp(4), dp(4), dp(4), dp(4));
            resultStickersGrid.addView(cell, glp);
        }

        resendEmailInput.setText(emailInput.getText().toString().trim());
        mainScrollView.post(new Runnable() {
            @Override
            public void run() {
                mainScrollView.smoothScrollTo(0, resultSectionCard.getTop());
            }
        });
    }

    private void shareSingleStickerBytes(byte[] pngBytes, String filename, String phrase) {
        try {
            File sharedDir = new File(getCacheDir(), "shared_files");
            if (!sharedDir.exists()) sharedDir.mkdirs();
            File outFile = new File(sharedDir, filename.replaceAll("[/\\\\]", "_"));
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(pngBytes);
            fos.close();

            Uri contentUri = StickerFileProvider.getUriForFile(outFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, phrase);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "이모티콘 공유하기"));
        } catch (Exception e) {
            Toast.makeText(this, "공유 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestInitialPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> needed = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.CAMERA);
            }
            if (Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else if (Build.VERSION.SDK_INT <= 28) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            if (!needed.isEmpty()) {
                requestPermissions(needed.toArray(new String[0]), REQ_PERMISSIONS);
            }
        }
    }

    private Intent createCameraCaptureIntent() {
        try {
            File sharedDir = new File(getCacheDir(), "shared_files");
            if (!sharedDir.exists()) sharedDir.mkdirs();
            cameraPhotoFile = new File(sharedDir, "capture_" + System.currentTimeMillis() + ".jpg");
            cameraPhotoUri = StickerFileProvider.getUriForFile(cameraPhotoFile);

            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return captureIntent;
        } catch (Exception e) {
            cameraPhotoUri = null;
            cameraPhotoFile = null;
            return null;
        }
    }

    private void launchNativeCameraCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                pendingNativeCameraLaunch = true;
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_PERMISSIONS);
                return;
            }
        }
        Intent captureIntent = createCameraCaptureIntent();
        if (captureIntent != null) {
            try {
                startActivityForResult(captureIntent, REQ_NATIVE_CAMERA);
            } catch (Exception e) {
                Toast.makeText(this, "카메라 실행 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchNativeGalleryPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "사진 앨범에서 선택"), REQ_NATIVE_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "갤러리 열기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS && pendingNativeCameraLaunch) {
            pendingNativeCameraLaunch = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchNativeCameraCapture();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_NATIVE_CAMERA && resultCode == Activity.RESULT_OK) {
            Uri targetUri = null;
            if (cameraPhotoFile != null && cameraPhotoFile.exists() && cameraPhotoFile.length() > 0) {
                targetUri = Uri.fromFile(cameraPhotoFile);
            } else if (data != null && data.getData() != null) {
                targetUri = data.getData();
            }
            if (targetUri != null) {
                loadSelectedPhotoUri(targetUri);
            }
        } else if (requestCode == REQ_NATIVE_GALLERY && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            loadSelectedPhotoUri(data.getData());
        }
    }

    private void loadSelectedPhotoUri(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is == null) return;
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bmp == null) return;

                    int maxDim = 1024;
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    if (Math.max(w, h) > maxDim) {
                        float scale = (float) maxDim / Math.max(w, h);
                        w = Math.round(w * scale);
                        h = Math.round(h * scale);
                        bmp = Bitmap.createScaledBitmap(bmp, w, h, true);
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, bos);
                    selectedPhotoBytes = bos.toByteArray();

                    final Bitmap finalBmp = bmp;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            photoPlaceholderText.setVisibility(View.GONE);
                            photoPreviewImage.setVisibility(View.VISIBLE);
                            photoPreviewImage.setImageBitmap(finalBmp);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "사진 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void writeBytesToDownloads(byte[] bytes, String mimeType, String fileName) throws Exception {
        String safeName = (fileName == null || fileName.isEmpty())
                ? ("sticker_" + System.currentTimeMillis() + ".png")
                : fileName.replaceAll("[/\\\\]", "_");

        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KakaoStickers");
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = resolver.openOutputStream(uri);
                if (os != null) {
                    os.write(bytes);
                    os.flush();
                    os.close();
                }
            }
        } else {
            File dlDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KakaoStickers");
            if (!dlDir.exists()) dlDir.mkdirs();
            File outFile = new File(dlDir, safeName);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(bytes);
            fos.flush();
            fos.close();
        }
    }

    private void stageFilesForEmailAndLaunchIntent(
            final OnDeviceStickerEngine.SliceResult sr,
            final String recipientEmail) {
        try {
            File sharedDir = new File(getCacheDir(), "shared_files");
            if (!sharedDir.exists()) sharedDir.mkdirs();

            ArrayList<Uri> uris = new ArrayList<>();

            File sheetFile = new File(sharedDir, "step3_sticker_sheet.png");
            FileOutputStream fosSheet = new FileOutputStream(sheetFile);
            fosSheet.write(sr.displaySheetPngBytes);
            fosSheet.close();
            uris.add(StickerFileProvider.getUriForFile(sheetFile));

            File zipFile = new File(sharedDir, "kakao_stickers_15pack.zip");
            FileOutputStream fosZip = new FileOutputStream(zipFile);
            fosZip.write(sr.zipBytes);
            fosZip.close();
            uris.add(StickerFileProvider.getUriForFile(zipFile));

            for (OnDeviceStickerEngine.SlicedSticker st : sr.stickers) {
                File stFile = new File(sharedDir, st.filename);
                FileOutputStream fosSt = new FileOutputStream(stFile);
                fosSt.write(st.pngBytes);
                fosSt.close();
                uris.add(StickerFileProvider.getUriForFile(stFile));
            }

            String bgLabel = "transparent".equalsIgnoreCase(sr.bgMode)
                    ? "투명 배경 (Transparent PNG)"
                    : "순수 흰색 배경 (#FFFFFF)";
            String subject = "[AI 이모티콘 스튜디오] " + lastMoodLabel + " · " + lastLanguageLabel
                    + " 15종 스티커 세트 (" + sr.stickerSize + "x" + sr.stickerSize + ")";
            String body = "안녕하세요,\n\n"
                    + "안드로이드 AI 이모티콘 스튜디오 앱에서 생성한 2D 카카오톡 스타일 이모티콘 15종 세트입니다.\n\n"
                    + "• 분위기: " + lastMoodLabel + "\n"
                    + "• 언어: " + lastLanguageLabel + "\n"
                    + "• 배경 모드: " + bgLabel + "\n"
                    + "• 크기: " + sr.stickerSize + "x" + sr.stickerSize + " px\n"
                    + "• 첨부 파일: 전체 3x5 시트 + 개별 PNG 15종 + 통합 ZIP 압축파일 (총 17개)\n";

            Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.setType("*/*");
            if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipientEmail.trim()});
            }
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.putExtra(Intent.EXTRA_TEXT, body);
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            if (!uris.isEmpty()) {
                ClipData clip = ClipData.newUri(getContentResolver(), "stickers", uris.get(0));
                for (int i = 1; i < uris.size(); i++) {
                    clip.addItem(new ClipData.Item(uris.get(i)));
                }
                sendIntent.setClipData(clip);
            }
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(sendIntent, "완성된 이모티콘 15종 공유 / 이메일 보내기");
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "공유 화면 열기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        public abstract void onChanged();

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            onChanged();
        }
    }
}
