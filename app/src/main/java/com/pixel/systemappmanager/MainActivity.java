package com.pixel.systemappmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
    private static final int BG = Color.rgb(247, 244, 255);
    private static final int NAVY = Color.rgb(35, 22, 89);
    private static final int INK = Color.rgb(44, 34, 78);
    private static final int MUTED = Color.rgb(108, 102, 142);
    private static final int MINT = Color.rgb(45, 155, 147);
    private static final int RED = Color.rgb(190, 67, 73);
    private static final int LAVENDER = Color.rgb(112, 106, 190);
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
                buildUi();
                checkRoot();
            }
        }, 1050);
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
        window.setNavigationBarColor(BG);
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void showSplash() {
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setPadding(dp(26), dp(24), dp(26), dp(24));
        splash.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(23, 18, 76), Color.rgb(55, 35, 125), Color.rgb(122, 48, 139)}));

        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.mascot);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout logoWrap = new LinearLayout(this);
        logoWrap.setGravity(Gravity.CENTER);
        logoWrap.setBackground(background(Color.rgb(30, 22, 86), 34, Color.rgb(226, 173, 255), dp(1)));
        logoWrap.setClipToOutline(true);
        logoWrap.setElevation(dp(10));
        logoWrap.addView(mascot, new LinearLayout.LayoutParams(dp(174), dp(174)));
        splash.addView(logoWrap, new LinearLayout.LayoutParams(dp(200), dp(200)));

        TextView brand = text("Pixel System", 30, WHITE, true);
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brandLp = params(0, 24, 0, 0);
        splash.addView(brand, brandLp);
        TextView brand2 = text("APP MANAGER", 15, Color.rgb(255, 174, 232), true);
        brand2.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brand2Lp = params(0, 0, 0, 8);
        splash.addView(brand2, brand2Lp);

        TextView slogan = text("프로필별 시스템 앱 관리 도우미", 14, Color.rgb(231, 220, 255), false);
        slogan.setGravity(Gravity.CENTER);
        splash.addView(slogan, params(0, 0, 0, 26));

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= 21) progress.setIndeterminateTintList(ColorStateList.valueOf(MINT));
        splash.addView(progress, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView loading = text("안전하게 시작하는 중…", 12, Color.rgb(218, 206, 245), false);
        loading.setGravity(Gravity.CENTER);
        splash.addView(loading, params(0, 14, 0, 0));

        setContentView(splash);
        logoWrap.setScaleX(0.88f);
        logoWrap.setScaleY(0.88f);
        logoWrap.setAlpha(0f);
        logoWrap.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420).start();
        brand.setAlpha(0f);
        brand.animate().alpha(1f).setDuration(500).setStartDelay(180).start();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.setBackgroundColor(BG);

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

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(13), dp(14), dp(13));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(36, 25, 93), Color.rgb(108, 52, 154)}));
        header.setElevation(dp(3));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.mascot);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setBackground(background(WHITE, 18, Color.TRANSPARENT, 0));
        icon.setClipToOutline(true);
        header.addView(icon, new LinearLayout.LayoutParams(dp(66), dp(66)));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setPadding(dp(12), 0, 0, 0);
        TextView title = text("Pixel System", 22, WHITE, true);
        TextView title2 = text("App Manager", 22, WHITE, true);
        TextView subtitle = text("프로필별 시스템 앱 관리", 12, Color.rgb(215, 222, 245), false);
        subtitle.setPadding(0, dp(5), 0, 0);
        headerText.addView(title);
        headerText.addView(title2);
        headerText.addView(subtitle);
        header.addView(headerText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header, params(0, 0, 0, 12));

        LinearLayout statusCard = horizontal();
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setPadding(dp(12), dp(9), dp(12), dp(9));
        statusCard.setBackground(background(WHITE, 16, Color.rgb(229, 233, 243), dp(1)));
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
        TextView profileLabel = text("사용자 프로필", 13, INK, true);
        profileCard.addView(profileLabel, params(0, 0, 0, 5));
        LinearLayout profileRow = horizontal();
        profileSpinner = new Spinner(this);
        profileRow.addView(profileSpinner, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button refresh = button("새로고침", Color.rgb(73, 91, 135), 14);
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
        search.setTextColor(INK);
        search.setHintTextColor(Color.rgb(139, 148, 166));
        search.setHint("⌕   패키지명 검색 (예: com.google)");
        search.setPadding(dp(15), dp(5), dp(15), dp(5));
        search.setBackground(background(WHITE, 15, Color.rgb(229, 233, 243), dp(1)));
        root.addView(search, params(0, 0, 0, 8));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterApps(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        LinearLayout summaryCard = card();
        TextView summaryLabel = text("패키지 현황", 12, MUTED, true);
        summaryCard.addView(summaryLabel, params(0, 0, 0, 3));
        summary = text("앱 목록을 불러오는 중…", 12, INK, false);
        summary.setLineSpacing(dp(2), 1f);
        summaryCard.addView(summary);
        root.addView(summaryCard, params(0, 0, 0, 8));

        Button selectVisible = button("표시된 항목 전체 선택 / 해제", Color.rgb(104, 115, 140), 13);
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
        listFrame.setBackground(background(WHITE, 18, Color.rgb(229, 233, 243), dp(1)));
        listFrame.setClipToOutline(true);
        listView = new ListView(this);
        listView.setDividerHeight(dp(1));
        listView.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(239, 242, 248)));
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
        emptyState = text("아직 표시할 시스템 패키지가 없어요.\n루트 권한을 허용하고 새로고침해 주세요. ♡",
                14, MUTED, false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setPadding(dp(20), dp(20), dp(20), dp(20));
        listFrame.addView(emptyState, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        listView.setEmptyView(emptyState);
        root.addView(listFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        log = text("안내  ·  앱은 선택한 프로필에서만 제거됩니다. 원본 APK 파일은 삭제하지 않습니다.",
                10, MUTED, false);
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
                    log.setText("KernelSU / Magisk / APatch에서 이 앱의 루트 권한을 허용한 뒤 새로고침해 주세요.");
                }
            });
        });
    }

    private void bindProfiles() {
        ArrayAdapter<Profile> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, profiles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        result.setPadding(dp(13), dp(11), dp(13), dp(11));
        result.setBackground(background(WHITE, 16, Color.rgb(229, 233, 243), dp(1)));
        result.setElevation(dp(1));
        return result;
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
        result.setBackground(background(color, 14, Color.TRANSPARENT, 0));
        result.setElevation(dp(2));
        result.setPadding(dp(5), 0, dp(5), 0);
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
            CheckBox check;
            TextView state;
            if (convertView instanceof LinearLayout && ((LinearLayout) convertView).getTag() instanceof View[]) {
                row = (LinearLayout) convertView;
                View[] refs = (View[]) row.getTag();
                check = (CheckBox) refs[0];
                state = (TextView) refs[1];
            } else {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(3), dp(8), dp(3));
                check = new CheckBox(MainActivity.this);
                check.setTextSize(12);
                check.setTextColor(INK);
                check.setPadding(0, dp(6), dp(5), dp(6));
                check.setButtonTintList(new ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                        new int[]{MINT, Color.rgb(151, 160, 177)}));
                row.addView(check, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                state = new TextView(MainActivity.this);
                state.setTextSize(10);
                state.setGravity(Gravity.CENTER);
                state.setPadding(dp(8), dp(6), dp(8), dp(6));
                row.addView(state, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setTag(new View[]{check, state});
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
                state.setTextColor(Color.rgb(154, 74, 56));
                state.setBackground(background(Color.rgb(255, 240, 234), 10, Color.TRANSPARENT, 0));
            } else if (item.installed) {
                state.setText("설치됨");
                state.setTextColor(MINT);
                state.setBackground(background(Color.rgb(229, 247, 240), 10, Color.TRANSPARENT, 0));
            } else {
                state.setText("제거됨");
                state.setTextColor(MUTED);
                state.setBackground(background(Color.rgb(239, 242, 248), 10, Color.TRANSPARENT, 0));
            }
            return row;
        }
    }
}
