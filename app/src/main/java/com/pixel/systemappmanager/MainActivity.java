package com.pixel.systemappmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Outline;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(27, 10, 70);
    private static final int NAVY = Color.rgb(35, 12, 82);
    private static final int INK = Color.rgb(255, 246, 255);
    private static final int MUTED = Color.rgb(224, 205, 250);
    private static final int MINT = Color.rgb(76, 255, 220);
    private static final int RED = Color.rgb(255, 66, 105);
    private static final int LAVENDER = Color.rgb(244, 171, 255);
    private static final int WHITE = Color.WHITE;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Profile> profiles = new ArrayList<>();
    private final ArrayList<AppItem> allApps = new ArrayList<>();
    private final ArrayList<AppItem> visibleApps = new ArrayList<>();
    private final Set<String> protectedPackages = new HashSet<>();

    private TextView rootStatus;
    private TextView summary;
    private TextView log;
    private TextView emptyState;
    private Spinner profileSpinner;
    private EditText search;
    private ListView listView;
    private AppAdapter adapter;
    private int selectedUser = 0;
    private boolean hasRoot = false;
    private boolean busy = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        protectedPackages.add("com.android.systemui");
        protectedPackages.add("com.android.settings");
        protectedPackages.add("com.android.providers.settings");
        protectedPackages.add("com.android.providers.telephony");
        protectedPackages.add("com.android.phone");
        protectedPackages.add("com.android.permissioncontroller");
        protectedPackages.add("com.google.android.permissioncontroller");
        showSplash();
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                configureSystemBars();
                try {
                    buildUi();
                    checkRoot();
                } catch (Throwable error) {
                    showStartupError(error);
                }
            }
        }, 650);
    }

    private void configureSystemBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // The system bars are transparent so the pearl gradient continues behind them.
        // Clearing LIGHT_STATUS_BAR / LIGHT_NAVIGATION_BAR keeps their glyphs white.
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void showSplash() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // A real full-bleed concept illustration—not a vector icon enlarged into a splash.
        FrameLayout splash = new FrameLayout(this);
        ImageView scene = new ImageView(this);
        scene.setImageResource(R.drawable.splash_art);
        scene.setScaleType(ImageView.ScaleType.CENTER_CROP);
        scene.setContentDescription("크리스털 안드로이드와 별빛 바다");
        splash.addView(scene, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View bottomShade = new View(this);
        GradientDrawable fade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00150A42, 0xA8150648, 0xEF16063B});
        bottomShade.setBackground(fade);
        FrameLayout.LayoutParams shadeLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(270), Gravity.BOTTOM);
        splash.addView(bottomShade, shadeLp);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_HORIZONTAL);
        copy.setPadding(dp(28), dp(20), dp(28), dp(34));
        TextView brand = text("Pixel System", 30, WHITE, true);
        brand.setGravity(Gravity.CENTER);
        copy.addView(brand, params(0, 0, 0, 0));
        TextView brand2 = text("APP MANAGER", 15, Color.rgb(255, 174, 232), true);
        brand2.setGravity(Gravity.CENTER);
        copy.addView(brand2, params(0, 0, 0, 8));
        TextView slogan = text("프로필별 시스템 앱 관리 도우미", 14,
                Color.rgb(245, 223, 255), false);
        slogan.setGravity(Gravity.CENTER);
        copy.addView(slogan, params(0, 0, 0, 18));
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= 21)
            progress.setIndeterminateTintList(ColorStateList.valueOf(Color.rgb(255, 190, 250)));
        copy.addView(progress, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView loading = text("시스템 앱을 더 안전하게 관리하세요", 12,
                Color.rgb(230, 207, 255), false);
        loading.setGravity(Gravity.CENTER);
        copy.addView(loading, params(0, 10, 0, 0));
        FrameLayout.LayoutParams copyLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        splash.addView(copy, copyLp);
        setContentView(splash);
        scene.setAlpha(0.72f);
        scene.animate().alpha(1f).setDuration(650).start();
        brand.setAlpha(0f);
        brand.animate().alpha(1f).setDuration(550).setStartDelay(160).start();
    }

    /**
     * Keep an actionable screen visible if an Android-version-specific view/resource
     * failure occurs during startup. The previous build could close before showing
     * any diagnostic when buildUi() threw.
     */
    private void showStartupError(Throwable error) {
        LinearLayout fallback = new LinearLayout(this);
        fallback.setOrientation(LinearLayout.VERTICAL);
        fallback.setGravity(Gravity.CENTER);
        fallback.setPadding(dp(24), dp(28), dp(24), dp(28));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(19, 8, 58), Color.rgb(116, 22, 130), Color.rgb(196, 37, 153)});
        fallback.setBackground(bg);

        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.icon_art);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fallback.addView(mascot, new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView title = text("시작 화면을 표시하지 못했습니다", 20, WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(16), 0, dp(12));
        fallback.addView(title);

        String detail = error == null ? "알 수 없는 오류" :
                error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
        TextView details = text(clip(detail, 420), 12, Color.rgb(255, 226, 252), false);
        details.setGravity(Gravity.CENTER);
        fallback.addView(details);

        Button retry = button("다시 시도", Color.rgb(83, 58, 185), 15);
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        retryLp.topMargin = dp(22);
        fallback.addView(retry, retryLp);
        retry.setOnClickListener(v -> {
            try {
                buildUi();
                checkRoot();
            } catch (Throwable retryError) {
                showStartupError(retryError);
            }
        });
        setContentView(fallback);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.setBackground(new PearlGradientDrawable());

        // Android 15+ may draw content edge-to-edge. Add system-bar insets to a fixed
        // base padding rather than letting the first heading sit behind the status bar.
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(dp(16) + bars.left, dp(12) + bars.top,
                        dp(16) + bars.right, dp(8) + bars.bottom);
                return insets;
            });
        }

        FrameLayout header = new FrameLayout(this);
        header.setMinimumHeight(dp(190));
        header.setBackground(new HeaderPearlDrawable());
        header.setClipToOutline(true);
        header.setElevation(dp(5));

        // The concept banner has the crystal-moon mascot on the left and clean text space at right.
        ImageView hero = new ImageView(this);
        hero.setImageResource(R.drawable.hero_art);
        hero.setScaleType(ImageView.ScaleType.FIT_XY);
        hero.setContentDescription("초승달 위의 크리스털 안드로이드와 은하수");
        header.addView(hero, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View textShade = new View(this);
        GradientDrawable rightShade = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x000D0638, 0x54240B62, 0xCA240747});
        textShade.setBackground(rightShade);
        header.addView(textShade, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setGravity(Gravity.CENTER_VERTICAL);
        headerText.setPadding(dp(4), dp(8), dp(16), dp(8));
        TextView title = text("Pixel System", 21, WHITE, true);
        TextView title2 = text("App Manager", 21, WHITE, true);
        TextView subtitle = text("프로필별 시스템 앱 관리", 11, Color.rgb(250, 226, 255), false);
        subtitle.setPadding(0, dp(5), 0, 0);
        headerText.addView(title);
        headerText.addView(title2);
        headerText.addView(subtitle);
        FrameLayout.LayoutParams heroCopy = new FrameLayout.LayoutParams(
                dp(198), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        headerText.setBackgroundColor(Color.TRANSPARENT);
        header.addView(headerText, heroCopy);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190)));
        ((LinearLayout.LayoutParams) header.getLayoutParams()).bottomMargin = dp(12);

        LinearLayout statusCard = horizontal();
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setPadding(dp(12), dp(9), dp(12), dp(9));
        statusCard.setBackground(background(0xBA29116D, 18, 0xF0E7FFFF, dp(1)));
        statusCard.setElevation(dp(4));
        TextView statusDot = text("●", 12, MINT, true);
        statusCard.addView(statusDot, new LinearLayout.LayoutParams(dp(20), dp(22)));
        rootStatus = text("루트 권한 확인 중…", 12, MUTED, true);
        statusCard.addView(rootStatus, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView rootTag = text("ROOT", 10, LAVENDER, true);
        rootTag.setPadding(dp(9), dp(5), dp(9), dp(5));
        rootTag.setBackground(background(Color.rgb(245, 232, 255), 12, Color.TRANSPARENT, 0));
        statusCard.addView(rootTag);
        root.addView(statusCard, params(0, 0, 0, 12));

        LinearLayout profileCard = card();
        TextView profileLabel = text("◈  사용자 프로필", 13, WHITE, true);
        profileCard.addView(profileLabel, params(0, 0, 0, 5));
        LinearLayout profileRow = horizontal();
        profileSpinner = new Spinner(this);
        profileRow.addView(profileSpinner, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button refresh = button("⟳  새로고침", Color.rgb(111, 59, 177), 14);
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(dp(100), dp(46));
        refreshLp.leftMargin = dp(8);
        profileRow.addView(refresh, refreshLp);
        refresh.setOnClickListener(v -> {
            if (hasRoot) loadPackages();
            else checkRoot();
        });
        profileCard.addView(profileRow);
        root.addView(profileCard, params(0, 0, 0, 10));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(WHITE);
        search.setHintTextColor(Color.rgb(226, 206, 250));
        search.setHint("⌕   패키지명 검색 (예: com.google)");
        search.setPadding(dp(15), dp(5), dp(15), dp(5));
        search.setBackground(background(0xBB7733A0, 18, 0xE8F2D9FF, dp(1)));
        search.setElevation(dp(2));
        root.addView(search, params(0, 0, 0, 8));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterApps(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        LinearLayout summaryCard = card();
        TextView summaryLabel = text("▣  패키지 현황", 12, LAVENDER, true);
        summaryCard.addView(summaryLabel, params(0, 0, 0, 3));
        summary = text("앱 목록을 불러오는 중…", 12, INK, false);
        summary.setLineSpacing(dp(2), 1f);
        summaryCard.addView(summary);
        root.addView(summaryCard, params(0, 0, 0, 8));

        Button selectVisible = button("▦  표시된 항목 전체 선택 / 해제", Color.rgb(79, 57, 178), 13);
        root.addView(selectVisible, params(0, 0, 0, 8));
        selectVisible.setOnClickListener(v -> {
            boolean select = false;
            for (AppItem item : visibleApps) if (!item.selected) { select = true; break; }
            for (AppItem item : visibleApps) item.selected = select;
            adapter.notifyDataSetChanged();
            updateSummary();
        });

        LinearLayout actionRow = horizontal();
        Button remove = button("선택 항목 제거", RED, 14);
        Button restore = button("선택 항목 복구", MINT, 14);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(48), 1f);
        half.rightMargin = dp(5);
        actionRow.addView(remove, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(48), 1f);
        half2.leftMargin = dp(5);
        actionRow.addView(restore, half2);
        root.addView(actionRow, params(0, 0, 0, 8));
        remove.setOnClickListener(v -> confirmOperation(true));
        restore.setOnClickListener(v -> confirmOperation(false));

        FrameLayout listFrame = new FrameLayout(this);
        listFrame.setBackground(background(0xB32D126B, 22, 0xE8F8E7FF, dp(1)));
        listFrame.setElevation(dp(3));
        listFrame.setClipToOutline(true);
        listView = new ListView(this);
        listView.setDividerHeight(dp(1));
        listView.setDivider(new android.graphics.drawable.ColorDrawable(0x46F7DFFF));
        listView.setSelector(android.R.color.transparent);
        listView.setPadding(0, dp(4), 0, dp(4));
        listView.setClipToPadding(false);
        adapter = new AppAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < visibleApps.size()) {
                visibleApps.get(position).selected = !visibleApps.get(position).selected;
                adapter.notifyDataSetChanged();
                updateSummary();
            }
        });
        listFrame.addView(listView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        emptyState = text("✦  아직 표시할 시스템 패키지가 없어요.  ✦\n루트 권한을 허용하고 새로고침해 주세요. ♡",
                14, MUTED, false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setPadding(dp(20), dp(20), dp(20), dp(20));
        listFrame.addView(emptyState, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        listView.setEmptyView(emptyState);
        root.addView(listFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        log = text("✧  안내 · 선택한 프로필에서만 제거됩니다. 원본 APK 파일은 삭제하지 않습니다.",
                10, Color.rgb(241, 221, 255), false);
        log.setGravity(Gravity.CENTER_VERTICAL);
        log.setPadding(dp(3), dp(8), dp(3), dp(2));
        root.addView(log, params(0, 0, 0, 0));
        setContentView(root);
        updateSummary();
    }

    private void checkRoot() {
        rootStatus.setText("루트 권한 확인 중…");
        rootStatus.setTextColor(MUTED);
        worker.execute(() -> {
            CmdResult result = runRoot("id");
            boolean root = result.exit == 0 && result.output.contains("uid=0");
            ArrayList<Profile> found = new ArrayList<>();
            if (root) found = parseUsers(runRoot("pm list users").output);
            if (found.isEmpty()) found.add(new Profile(0, "Owner"));
            Collections.sort(found, Comparator.comparingInt(p -> p.id));
            final boolean rootReady = root;
            final ArrayList<Profile> userList = found;
            runOnUiThread(() -> {
                hasRoot = rootReady;
                profiles.clear();
                profiles.addAll(userList);
                rootStatus.setText(rootReady
                        ? "루트 권한 사용 가능 · " + profiles.size() + "개 프로필"
                        : "루트 권한 없음 · su 접근을 허용해 주세요");
                rootStatus.setTextColor(rootReady ? MINT : RED);
                bindProfiles();
                if (hasRoot) loadPackages();
                else {
                    allApps.clear();
                    filterApps();
                    String diagnostic = result.output == null || result.output.trim().isEmpty()
                            ? "출력 없음 (종료 코드 " + result.exit + ")"
                            : clip(result.output.trim(), 320);
                    log.setText("루트 확인 결과: " + diagnostic +
                            "\nKernelSU / Magisk / APatch의 앱 권한 목록에서 이 앱에 루트 권한을 허용한 뒤 새로고침해 주세요.");
                }
            });
        });
    }

    private void bindProfiles() {
        ArrayAdapter<Profile> spinnerAdapter = new ArrayAdapter<Profile>(
                this, android.R.layout.simple_spinner_item, profiles) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView item = (TextView) super.getView(position, convertView, parent);
                item.setTextColor(WHITE);
                item.setTextSize(14);
                item.setPadding(dp(10), dp(8), dp(10), dp(8));
                return item;
            }

            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView item = (TextView) super.getDropDownView(position, convertView, parent);
                item.setTextColor(WHITE);
                item.setTextSize(14);
                item.setPadding(dp(14), dp(13), dp(14), dp(13));
                item.setBackgroundColor(Color.rgb(48, 18, 94));
                return item;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setBackground(background(0xB56D389E, 16, 0xE8F6DDFF, dp(1)));
        profileSpinner.setPadding(dp(6), 0, dp(6), 0);
        profileSpinner.setPopupBackgroundDrawable(background(Color.rgb(48, 18, 94), 16,
                Color.rgb(219, 167, 255), dp(1)));
        profileSpinner.setAdapter(spinnerAdapter);
        int index = 0;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id == selectedUser) { index = i; break; }
        }
        selectedUser = profiles.get(index).id;
        profileSpinner.setSelection(index, false);
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < profiles.size()) {
                    int idValue = profiles.get(position).id;
                    if (selectedUser != idValue) {
                        selectedUser = idValue;
                        loadPackages();
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadPackages() {
        if (!hasRoot || busy) return;
        busy = true;
        final int user = selectedUser;
        summary.setText("user " + user + " 패키지 목록을 읽는 중…");
        worker.execute(() -> {
            CmdResult universeResult = runRoot("pm list packages -s -u --user " + user);
            CmdResult installedResult = runRoot("pm list packages -s --user " + user);
            Set<String> universe = parsePackages(universeResult.output);
            Set<String> installed = parsePackages(installedResult.output);
            ArrayList<AppItem> loaded = new ArrayList<>();
            for (String name : universe) loaded.add(new AppItem(name, installed.contains(name)));
            Collections.sort(loaded, Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
            runOnUiThread(() -> {
                busy = false;
                if (user != selectedUser) { loadPackages(); return; }
                allApps.clear();
                allApps.addAll(loaded);
                if (loaded.isEmpty()) {
                    log.setText("패키지 목록을 읽지 못했습니다. su 권한과 pm 명령 결과를 확인하세요.\n" +
                            clip(universeResult.output + "\n" + installedResult.output, 500));
                } else {
                    log.setText("목록을 갱신했습니다. 앱을 선택한 뒤 제거 또는 복구를 진행하세요.");
                }
                filterApps();
            });
        });
    }

    private void filterApps() {
        String query = search == null || search.getText() == null
                ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        visibleApps.clear();
        for (AppItem app : allApps) {
            if (query.isEmpty() || app.name.toLowerCase(Locale.ROOT).contains(query)) visibleApps.add(app);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateSummary();
    }

    private void updateSummary() {
        if (summary == null) return;
        int installed = 0, removed = 0, selected = 0;
        for (AppItem app : allApps) {
            if (app.installed) installed++; else removed++;
            if (app.selected) selected++;
        }
        summary.setText("user " + selectedUser + "    •    표시 " + visibleApps.size() +
                " / " + allApps.size() + "\n설치됨 " + installed + "    •    제거됨 " + removed +
                "    •    선택 " + selected);
    }

    private void confirmOperation(boolean uninstall) {
        ArrayList<AppItem> selected = new ArrayList<>();
        int blocked = 0;
        for (AppItem item : allApps) {
            if (!item.selected) continue;
            if (uninstall && protectedPackages.contains(item.name)) { blocked++; continue; }
            if (uninstall ? item.installed : !item.installed) selected.add(item);
        }
        if (selected.isEmpty()) {
            log.setText(uninstall
                    ? "제거할 대상이 없습니다. 이미 제거된 앱이거나 핵심 보호 패키지일 수 있습니다."
                    : "복구할 대상이 없습니다. 선택한 앱이 이미 설치되어 있습니다.");
            return;
        }
        String action = uninstall ? "제거" : "복구";
        String message = "대상 프로필: " + profileLabel() + "\n작업: " + action +
                "\n처리 대상: " + selected.size() + "개\n" +
                (blocked > 0 ? "핵심 보호 패키지로 차단된 항목: " + blocked + "개\n" : "") +
                (uninstall
                    ? "\n이 작업은 선택한 프로필에서 앱을 제거합니다. 시스템 모듈과 원본 APK는 삭제하지 않지만 일부 기능이 작동하지 않을 수 있습니다. 계속할까요?"
                    : "\n선택한 프로필에서 기존 패키지를 다시 사용할 수 있도록 등록합니다. 계속할까요?");
        new AlertDialog.Builder(this).setTitle("선택 항목 " + action).setMessage(message)
                .setNegativeButton("취소", null)
                .setPositiveButton(action, (dialog, which) -> runOperation(selected, uninstall))
                .show();
    }

    private void runOperation(ArrayList<AppItem> items, boolean uninstall) {
        if (busy) return;
        busy = true;
        final int user = selectedUser;
        ArrayList<AppItem> targets = new ArrayList<>(items);
        StringBuilder commands = new StringBuilder("set -f; ");
        for (AppItem app : targets) {
            if (!app.name.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) continue;
            commands.append("echo '=== ").append(app.name).append(" ==='; ");
            if (uninstall) commands.append("pm uninstall --user ").append(user).append(" ").append(app.name);
            else commands.append("cmd package install-existing --user ").append(user).append(" ").append(app.name);
            commands.append("; ");
        }
        log.setText("작업 실행 중…");
        worker.execute(() -> {
            CmdResult result = runRoot(commands.toString());
            String output = result.output.trim().isEmpty()
                    ? "명령 출력 없음. 종료 코드: " + result.exit : result.output;
            runOnUiThread(() -> {
                busy = false;
                log.setText(actionWord(uninstall) + " 명령 완료 · user " + user);
                new AlertDialog.Builder(this).setTitle(actionWord(uninstall) + " 결과")
                        .setMessage(clip(output, 9000))
                        .setPositiveButton("확인", null).show();
                loadPackages();
            });
        });
    }

    private String actionWord(boolean remove) { return remove ? "제거" : "복구"; }

    private String profileLabel() {
        for (Profile profile : profiles) if (profile.id == selectedUser) {
            return profile.name + " (user " + profile.id + ")";
        }
        return "user " + selectedUser;
    }

    private ArrayList<Profile> parseUsers(String output) {
        ArrayList<Profile> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("UserInfo\\{(\\d+):([^:}]+):").matcher(output);
        while (matcher.find()) {
            try {
                int id = Integer.parseInt(matcher.group(1));
                String name = matcher.group(2).trim();
                boolean duplicate = false;
                for (Profile p : result) if (p.id == id) duplicate = true;
                if (!duplicate) result.add(new Profile(id, name.isEmpty() ? "User" : name));
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private Set<String> parsePackages(String output) {
        Set<String> result = new HashSet<>();
        for (String line : output.split("\\r?\\n")) {
            line = line.trim();
            if (line.startsWith("package:")) {
                String name = line.substring(8).trim();
                if (name.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) result.add(name);
            }
        }
        return result;
    }

    private CmdResult runRoot(String command) {
        StringBuilder output = new StringBuilder();
        int exit = -1;
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() < 12000) output.append(line).append('\n');
                }
            }
            exit = process.waitFor();
        } catch (Exception error) {
            output.append(error.getClass().getSimpleName()).append(": ").append(error.getMessage());
            if (process != null) process.destroy();
        }
        return new CmdResult(output.toString(), exit);
    }

    private LinearLayout card() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(dp(14), dp(12), dp(14), dp(12));
        result.setBackground(background(0xB72D126E, 20, 0xE8F7E7FF, dp(1)));
        result.setElevation(dp(3));
        return result;
    }

    private int lighten(int color, float amount) {
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        return Color.rgb((int)(r + (255 - r) * amount),
                (int)(g + (255 - g) * amount), (int)(b + (255 - b) * amount));
    }

    private int darken(int color, float amount) {
        return Color.rgb((int)(Color.red(color) * (1f - amount)),
                (int)(Color.green(color) * (1f - amount)),
                (int)(Color.blue(color) * (1f - amount)));
    }

    private GradientDrawable background(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(size);
        result.setTextColor(color);
        if (bold) result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return result;
    }

    private Button button(String label, int color, float size) {
        Button result = new Button(this);
        result.setText(label);
        result.setTextSize(size);
        result.setAllCaps(false);
        result.setTextColor(WHITE);
        result.setGravity(Gravity.CENTER);
        int start;
        int end;
        if (color == RED) {
            start = Color.rgb(255, 82, 119);
            end = Color.rgb(197, 35, 96);
        } else if (color == MINT) {
            start = Color.rgb(25, 211, 191);
            end = Color.rgb(28, 138, 177);
        } else if (color == Color.rgb(79, 57, 178)) {
            start = Color.rgb(53, 68, 190);
            end = Color.rgb(139, 48, 190);
        } else {
            start = lighten(color, 0.18f);
            end = darken(color, 0.12f);
        }
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{start, end});
        fill.setCornerRadius(dp(16));
        fill.setStroke(dp(1), 0x7FFFFFFF);
        result.setBackground(fill);
        result.setElevation(dp(4));
        result.setPadding(dp(7), 0, dp(7), 0);
        return result;
    }

    private LinearLayout.LayoutParams params(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams result = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        result.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return result;
    }

    private String clip(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }

    /** Dark, star-flecked violet-magenta glass used behind the hero mascot. */
    private static class HeaderPearlDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float w = b.width(), h = b.height();
            if (w <= 0 || h <= 0) return;
            paint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                    new int[]{Color.rgb(24, 10, 70), Color.rgb(76, 19, 121), Color.rgb(163, 35, 161)},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(b, paint);
            paint.setShader(new RadialGradient(b.left + w * 0.18f, b.top + h * 0.25f,
                    w * 0.65f, new int[]{0x42B9A8FF, 0x12D2A8FF, 0x00FFFFFF},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(b, paint);
            paint.setShader(new RadialGradient(b.left + w * 0.9f, b.top + h * 0.88f,
                    w * 0.55f, new int[]{0x42FF9FE8, 0x10FFB8F1, 0x00FFFFFF},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(b, paint);
            paint.setShader(null);
            float[][] stars = {
                    {0.08f,0.15f},{0.22f,0.72f},{0.36f,0.20f},{0.51f,0.12f},
                    {0.64f,0.78f},{0.79f,0.22f},{0.91f,0.13f},{0.94f,0.75f},
                    {0.16f,0.88f},{0.43f,0.85f},{0.72f,0.48f},{0.56f,0.46f}
            };
            for (int i=0; i<stars.length; i++) {
                float x=b.left+w*stars[i][0], y=b.top+h*stars[i][1];
                float r=Math.max(1.2f,w*(i%3==0?0.006f:0.0028f));
                paint.setColor(i%3==0?0xD8FFF5FF:0x78EDE5FF);
                canvas.drawCircle(x,y,r,paint);
                if(i%3==0) {
                    paint.setColor(0x9AFFFFFF);
                    canvas.drawLine(x-r*3.5f,y,x+r*3.5f,y,paint);
                    canvas.drawLine(x,y-r*3.5f,x,y+r*3.5f,paint);
                    paint.setColor(0x55FFBFF4);
                    canvas.drawCircle(x,y,r*2.5f,paint);
                }
            }
            paint.setShader(null);
            paint.setColor(Color.WHITE);
        }
        @Override public void getOutline(Outline outline) {
            android.graphics.Rect b=getBounds();
            outline.setRoundRect(b, Math.min(b.width(), b.height()) * 0.065f);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    /**
     * A vertical amethyst-to-magenta backdrop with subtle pearlescent light blooms
     * and restrained sparkle flecks. It is drawn full-bleed behind transparent
     * system bars and does not use any external image or network resource.
     */
    private static class PearlGradientDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] colors = new int[]{
                Color.rgb(27, 10, 70),
                Color.rgb(76, 22, 116),
                Color.rgb(156, 31, 133),
                Color.rgb(224, 56, 174)
        };
        private final float[] stops = new float[]{0f, 0.38f, 0.73f, 1f};

        @Override protected void onBoundsChange(android.graphics.Rect bounds) {
            super.onBoundsChange(bounds);
            paint.setShader(new LinearGradient(
                    bounds.left, bounds.top, bounds.left, bounds.bottom,
                    colors, stops, Shader.TileMode.CLAMP));
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            float width = bounds.width();
            float height = bounds.height();
            if (width <= 0 || height <= 0) return;

            paint.setShader(new LinearGradient(
                    bounds.left, bounds.top, bounds.left, bounds.bottom,
                    colors, stops, Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(bounds, paint);

            // Soft pearl reflections; deliberately low opacity to preserve readability.
            drawBloom(canvas, bounds.left + width * 0.12f, bounds.top + height * 0.20f,
                    width * 0.68f, 0x48F1DDFF, 0x1AF3C9FF);
            drawBloom(canvas, bounds.left + width * 0.92f, bounds.top + height * 0.47f,
                    width * 0.62f, 0x42FFC6F0, 0x12FFFFFF);
            drawBloom(canvas, bounds.left + width * 0.43f, bounds.top + height * 0.88f,
                    width * 0.76f, 0x3BFFF0FF, 0x08FFFFFF);
            drawBloom(canvas, bounds.left + width * 0.56f, bounds.top + height * 0.59f,
                    width * 0.40f, 0x22C7B5FF, 0x05FFFFFF);

            paint.setShader(null);
            float[][] flecks = new float[][]{
                    {0.05f,0.08f},{0.14f,0.16f},{0.26f,0.09f},{0.39f,0.18f},
                    {0.54f,0.08f},{0.69f,0.13f},{0.83f,0.07f},{0.95f,0.18f},
                    {0.09f,0.29f},{0.31f,0.33f},{0.50f,0.27f},{0.74f,0.31f},
                    {0.91f,0.39f},{0.18f,0.46f},{0.42f,0.48f},{0.64f,0.43f},
                    {0.84f,0.53f},{0.07f,0.63f},{0.27f,0.68f},{0.49f,0.60f},
                    {0.70f,0.69f},{0.94f,0.65f},{0.15f,0.81f},{0.36f,0.87f},
                    {0.58f,0.80f},{0.80f,0.87f},{0.95f,0.94f},{0.06f,0.96f}
            };
            for (int i = 0; i < flecks.length; i++) {
                float x = bounds.left + width * flecks[i][0];
                float y = bounds.top + height * flecks[i][1];
                float radius = Math.max(1.2f, width * (i % 4 == 0 ? 0.0048f : 0.0026f));
                paint.setColor(i % 4 == 0 ? 0xB0FFF0FF : 0x68FFF8FF);
                canvas.drawCircle(x, y, radius, paint);
                if (i % 4 == 0) {
                    paint.setColor(0x92FFFFFF);
                    canvas.drawLine(x - radius * 3.3f, y, x + radius * 3.3f, y, paint);
                    canvas.drawLine(x, y - radius * 3.3f, x, y + radius * 3.3f, paint);
                    paint.setColor(0x40FBC7FF);
                    canvas.drawCircle(x, y, radius * 2.2f, paint);
                }
            }
            paint.setColor(Color.WHITE);
            paint.setShader(null);
        }

        private void drawBloom(Canvas canvas, float cx, float cy, float radius,
                               int centerColor, int midColor) {
            paint.setShader(new RadialGradient(
                    cx, cy, radius,
                    new int[]{centerColor, midColor, 0x00FFFFFF},
                    new float[]{0f, 0.48f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    private static class Profile {
        final int id;
        final String name;
        Profile(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name + " (user " + id + ")"; }
    }

    private static class AppItem {
        final String name;
        final boolean installed;
        boolean selected;
        AppItem(String name, boolean installed) { this.name = name; this.installed = installed; }
    }

    private static class CmdResult {
        final String output;
        final int exit;
        CmdResult(String output, int exit) { this.output = output == null ? "" : output; this.exit = exit; }
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return visibleApps.size(); }
        @Override public Object getItem(int position) { return visibleApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            AppItem item = visibleApps.get(position);
            LinearLayout row;
            ImageView appIcon;
            CheckBox check;
            TextView state;
            if (convertView instanceof LinearLayout && ((LinearLayout) convertView).getTag() instanceof View[]) {
                row = (LinearLayout) convertView;
                View[] refs = (View[]) row.getTag();
                appIcon = (ImageView) refs[0];
                check = (CheckBox) refs[1];
                state = (TextView) refs[2];
            } else {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(5), dp(8), dp(5));

                appIcon = new ImageView(MainActivity.this);
                appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                appIcon.setPadding(dp(3), dp(3), dp(3), dp(3));
                appIcon.setBackground(background(0x553A2580, 10, 0x88E9C9FF, dp(1)));
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(34), dp(34));
                iconLp.rightMargin = dp(8);
                row.addView(appIcon, iconLp);

                check = new CheckBox(MainActivity.this);
                check.setTextSize(12);
                check.setTextColor(WHITE);
                check.setPadding(0, dp(6), dp(5), dp(6));
                check.setButtonTintList(new ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                        new int[]{Color.rgb(255, 135, 237), Color.rgb(234, 218, 255)}));
                row.addView(check, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                state = new TextView(MainActivity.this);
                state.setTextSize(10);
                state.setGravity(Gravity.CENTER);
                state.setPadding(dp(8), dp(6), dp(8), dp(6));
                row.addView(state, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setTag(new View[]{appIcon, check, state});
            }
            try {
                appIcon.setImageDrawable(getPackageManager().getApplicationIcon(item.name));
            } catch (Exception ignored) {
                appIcon.setImageResource(R.drawable.icon_art);
            }
            check.setOnCheckedChangeListener(null);
            check.setText(item.name);
            check.setChecked(item.selected);
            check.setOnCheckedChangeListener((buttonView, checked) -> {
                item.selected = checked;
                updateSummary();
            });
            if (protectedPackages.contains(item.name)) {
                state.setText("핵심 보호");
                state.setTextColor(Color.rgb(255, 224, 238));
                state.setBackground(background(0x668D305F, 10, 0x66FFC1E8, dp(1)));
            } else if (item.installed) {
                state.setText("설치됨");
                state.setTextColor(Color.rgb(153, 255, 236));
                state.setBackground(background(0x5533A999, 10, 0x7A9CFFED, dp(1)));
            } else {
                state.setText("제거됨");
                state.setTextColor(Color.rgb(249, 195, 255));
                state.setBackground(background(0x5D7F3198, 10, 0x8CECB4FF, dp(1)));
            }
            return row;
        }
    }
}
