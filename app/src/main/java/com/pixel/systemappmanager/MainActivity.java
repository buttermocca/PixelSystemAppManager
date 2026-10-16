package com.pixel.systemappmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Profile> profiles = new ArrayList<>();
    private final ArrayList<AppItem> allApps = new ArrayList<>();
    private final ArrayList<AppItem> visibleApps = new ArrayList<>();
    private final Set<String> protectedPackages = new HashSet<>();

    private TextView rootStatus;
    private TextView summary;
    private TextView log;
    private Spinner profileSpinner;
    private EditText search;
    private ListView listView;
    private AppAdapter adapter;
    private int selectedUser = 0;
    private boolean hasRoot = false;
    private boolean busy = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        protectedPackages.add("com.android.systemui");
        protectedPackages.add("com.android.settings");
        protectedPackages.add("com.android.providers.settings");
        protectedPackages.add("com.android.providers.telephony");
        protectedPackages.add("com.android.phone");
        protectedPackages.add("com.android.permissioncontroller");
        protectedPackages.add("com.google.android.permissioncontroller");
        buildUi();
        checkRoot();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(8));
        root.setBackgroundColor(Color.rgb(244, 246, 250));

        TextView title = text("Pixel System App Manager", 22, Color.rgb(25, 38, 60), true);
        root.addView(title, params(0, 0, 0, 3));
        TextView subtitle = text("루팅된 픽셀폰 · 프로필별 시스템 앱 관리", 13,
                Color.rgb(86, 99, 120), false);
        root.addView(subtitle, params(0, 0, 0, 8));

        rootStatus = text("루트 권한 확인 중…", 12, Color.rgb(50, 90, 150), true);
        root.addView(rootStatus, params(0, 0, 0, 8));

        LinearLayout profileRow = horizontal();
        TextView profileLabel = text("프로필", 14, Color.rgb(40, 52, 70), true);
        profileRow.addView(profileLabel, new LinearLayout.LayoutParams(dp(54), dp(46)));
        profileSpinner = new Spinner(this);
        profileRow.addView(profileSpinner, new LinearLayout.LayoutParams(0, dp(46), 1f));
        Button refresh = button("새로고침", Color.rgb(57, 82, 120));
        profileRow.addView(refresh, new LinearLayout.LayoutParams(dp(88), dp(44)));
        refresh.setOnClickListener(v -> {
            if (hasRoot) loadPackages();
            else checkRoot();
        });
        root.addView(profileRow, params(0, 0, 0, 5));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("패키지명 검색 (예: com.google)");
        search.setPadding(dp(12), dp(3), dp(12), dp(3));
        search.setBackgroundColor(Color.WHITE);
        root.addView(search, params(0, 0, 0, 5));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterApps(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        summary = text("앱 목록을 불러오는 중…", 11, Color.rgb(86, 99, 120), false);
        root.addView(summary, params(0, 0, 0, 4));

        Button selectVisible = button("표시된 항목 전체 선택 / 해제", Color.rgb(100, 111, 130));
        root.addView(selectVisible, params(0, 0, 0, 4));
        selectVisible.setOnClickListener(v -> {
            boolean select = false;
            for (AppItem item : visibleApps) if (!item.selected) { select = true; break; }
            for (AppItem item : visibleApps) item.selected = select;
            adapter.notifyDataSetChanged();
            updateSummary();
        });

        LinearLayout actionRow = horizontal();
        Button remove = button("선택 항목 제거", Color.rgb(183, 61, 61));
        Button restore = button("선택 항목 복구", Color.rgb(35, 120, 89));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(46), 1f);
        half.rightMargin = dp(4);
        actionRow.addView(remove, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        half2.leftMargin = dp(4);
        actionRow.addView(restore, half2);
        root.addView(actionRow, params(0, 0, 0, 5));
        remove.setOnClickListener(v -> confirmOperation(true));
        restore.setOnClickListener(v -> confirmOperation(false));

        listView = new ListView(this);
        listView.setDividerHeight(dp(1));
        adapter = new AppAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < visibleApps.size()) {
                visibleApps.get(position).selected = !visibleApps.get(position).selected;
                adapter.notifyDataSetChanged();
                updateSummary();
            }
        });
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        log = text("안내: 앱은 선택한 프로필에서만 제거됩니다. 시스템 파티션의 APK 파일은 삭제하지 않습니다.",
                11, Color.rgb(86, 99, 120), false);
        log.setPadding(dp(2), dp(6), dp(2), dp(2));
        root.addView(log, params(0, 0, 0, 0));
        setContentView(root);
    }

    private void checkRoot() {
        rootStatus.setText("루트 권한 확인 중… su 요청이 나타나면 허용하세요.");
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
                        ? "루트 권한 확인됨 · " + profiles.size() + "개 사용자 / 프로필"
                        : "루트 권한 없음 · Magisk / KernelSU / APatch에서 su 권한을 허용하세요.");
                rootStatus.setTextColor(rootReady ? Color.rgb(29, 105, 75) : Color.rgb(150, 51, 51));
                bindProfiles();
                if (hasRoot) loadPackages();
                else {
                    allApps.clear();
                    filterApps();
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
                    log.setText("패키지 목록을 읽지 못했습니다. 루트 권한과 pm 명령 결과를 확인하세요.\n" +
                            clip(universeResult.output + "\n" + installedResult.output, 500));
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
        summary.setText("user " + selectedUser + " · 표시 " + visibleApps.size() +
                " / 시스템 패키지 " + allApps.size() + " · 설치됨 " + installed +
                " · 제거됨 " + removed + " · 선택 " + selected);
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
                    ? "\n이 작업은 선택한 프로필에서 앱을 제거합니다. 시스템 APK는 삭제하지 않지만 일부 기능이 작동하지 않을 수 있습니다. 계속할까요?"
                    : "\n선택한 프로필에서 해당 앱을 다시 사용할 수 있도록 복구합니다. 계속할까요?");
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
        if (bold) result.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return result;
    }

    private Button button(String label, int color) {
        Button result = new Button(this);
        result.setText(label);
        result.setTextSize(12);
        result.setAllCaps(false);
        result.setTextColor(Color.WHITE);
        result.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        result.setPadding(dp(3), 0, dp(3), 0);
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

        @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
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
                row.setPadding(dp(6), dp(3), dp(6), dp(3));
                check = new CheckBox(MainActivity.this);
                check.setTextSize(12);
                check.setPadding(0, dp(4), dp(4), dp(4));
                row.addView(check, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                state = new TextView(MainActivity.this);
                state.setTextSize(10);
                state.setGravity(Gravity.CENTER);
                state.setPadding(dp(5), dp(5), dp(5), dp(5));
                row.addView(state, new LinearLayout.LayoutParams(dp(76), LinearLayout.LayoutParams.WRAP_CONTENT));
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
                state.setTextColor(Color.rgb(148, 67, 42));
            } else if (item.installed) {
                state.setText("설치됨");
                state.setTextColor(Color.rgb(31, 107, 78));
            } else {
                state.setText("제거됨");
                state.setTextColor(Color.rgb(90, 98, 113));
            }
            return row;
        }
    }
}
