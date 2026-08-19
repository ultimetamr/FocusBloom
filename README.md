# Focus Bloom · 空间番茄钟

Focus Bloom 是面向 PICO OS 6 Shared Space 的中文 planar 空间番茄钟。用户写下一件主任务，把任务卡放入花盆后开始 5、15 或 25 分钟专注；花朵按进度分阶段成长，干扰事项可吸附进“稍后再说”收纳盒。

## 当前 MVP

- 首页、编辑态、专注态、暂停菜单、完成页和最近 14 天花园。
- 本地计时与 SharedPreferences 持久化，支持暂停、恢复和跨日期恢复。
- 主任务卡与干扰卡拖放命中判定，并保留点击按钮作为模拟器/控制器回退。
- 程序化低模花盆与五阶段花朵，不依赖外部资产。
- 完成页可导出截图到 `Pictures/FocusBloom`。

## 构建

```text
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

应用包名为 `com.pico.swan.focusbloom`，入口 Activity 为 `.platform.LaunchActivity`。
