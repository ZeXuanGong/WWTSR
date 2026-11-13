# WWTSR

[English](README.md) | 简体中文

用 Java（JavaFX）写的桌面宠物：一只小家伙待在屏幕角落，每次按键或点击鼠标时左右手交替敲击。
敲击可以解锁宝箱；宝箱里开出角色、配件和表情，可以收集、佩戴和合成。

所有美术都是用代码绘制的原创占位图（`GeneratedAssetPack`）；要替换它，另写一个 `AssetPack` 即可。

## 运行

需要 JDK 25 或更新版本，以及 Maven 3.9。

```sh
mvn test                                  # 单元测试
mvn javafx:run                            # 启动桌宠
mvn javafx:run -Djavafx.args=--demo       # 启动后自己敲击（不监听输入，不保存任何内容）
```

不打开窗口、直接画成 PNG 文件的开发工具：

```sh
mvn javafx:run -Dmain.class=com.wwtsr/com.wwtsr.app.ContactSheet -Djavafx.args=sheet.png
mvn javafx:run -Dmain.class=com.wwtsr/com.wwtsr.app.PanelSheet "-Djavafx.args=outdir zh"
```

**调试版与发布版。** `mvn javafx:run` 是调试运行：它会设置 `-Dwwtsr.debug=true`，打开一个看门狗，
界面 3 秒没有响应时，把当时每个线程在做什么写进数据文件夹里的 `watchdog.log`。打包出的发布版不监视界面，
也不写日志。两者都会在 Windows 丢掉输入钩子时重新安装钩子；只有调试版会把这件事记下来。

## 打包

```powershell
$env:JAVA_HOME = '<JDK 25 或更新版本的路径>'
powershell -ExecutionPolicy Bypass -File package.ps1 -Maven <mvn.cmd 的路径>
```

脚本先运行测试，然后生成 `target\dist\WWTSR\`（`WWTSR.exe` 加上自带的 Java 运行时，不需要安装任何东西）
和 `target\dist\WWTSR-<版本>-windows-x64.zip`。要把应用给别人，发这个 zip 即可；对方解压到任意位置，
运行 `WWTSR.exe`。

## 使用

**桌宠。** 拖动可以移动它，它会记住你把它放在哪里。单击它，表情会沿它左侧的一道弧线飞出来；
点其中一个就冒出这个表情，再单击桌宠把它们收起。右键单击桌宠，或系统托盘里它的图标，可以打开菜单。

**宝箱。** 有两个宝箱位。开一个宝箱要花 1,000 次敲击，从桌宠计数条上的数字里扣除
（设置里显示的是累计次数，它不会减少）。有 1,000 次敲击时显示一个宝箱；有 2,000 次时两个都显示，
可以接连打开。一个宝箱开出一件随机收集物（普通、优秀、稀有或史诗），在桌宠上方揭晓，越稀有特效越多；
这个宝箱位 30 分钟后再次就绪。还在等待的宝箱位只在鼠标停在桌宠上时带着倒计时显示，除非你选择总是显示；
敲击不足 1,000 次时，任何宝箱都不显示。

**面板**（从菜单打开，或点计数条上的 ≡ 按钮）有四页：

- **背包**：你拥有的全部收集物，按页签（最近获得、角色、配件、表情）和筛选条件（系列、稀有度）查看。
  点一张卡片就使用它：角色成为桌宠的形象，配件戴上或摘下，表情放进五个表情槽之一。喜欢的可以标星。
  五个预设各保存一整套形象（角色 + 配件 + 表情）。
- **图鉴**：全部收集物，还没获得的画得很淡。
- **合成**：把 10 份同类别、同稀有度的多余收集物放进格子，合成一件高一级稀有度的；
  或放 5 份，合成一件同稀有度的；“一键选择”会替你填满格子。只会用掉多余的份数：每样东西你总会留下一份。
- **设置**：昵称、大小、不透明度、屏幕、语言（中文 / English）、锁定位置、鼠标悬停时变淡、
  总是显示宝箱、游戏模式、任务栏按钮、随 Windows 启动（仅安装版可用），以及三个快捷键。

**快捷键。** F9 显示或隐藏桌宠，F8 把它移到下一块屏幕，F7 开关游戏模式；都可以在设置里更改或清除。
快捷键可以是单独一个功能键，也可以是按住 Ctrl、Alt 或 Win 再加另一个键（还可以加上 Shift）。
它们在任何地方都有效，并且不会从你正在使用的程序那里抢走按键。

**拖动。** 拖动时桌宠不会离开它所在的屏幕，松手后会落到任务栏上方。它的位置按占屏幕的比例记住，
所以屏幕尺寸变了、或桌宠移到另一块屏幕之后，它会落在对应的位置。

**游戏模式。** 桌宠仍在屏幕上，但鼠标会直接穿过它，有宝箱可开时除外。默认情况下，
前台有全屏程序时它会自动打开。

**它监听什么。** 在 Windows 上它安装低级键盘钩子和鼠标钩子。它只向外传递“按了一个键”或“点了一下鼠标”：
具体是哪个键只在一瞬间用到，用来忽略按住按键时的重复信号和识别快捷键，绝不保存、不写日志、
也不发送到任何地方。以管理员身份运行的窗口里的按键听不到，除非 WWTSR 也以管理员身份运行。

**它保存什么**，位置在 `%APPDATA%\WWTSR\`：`taps.properties`（两个计数）、`settings.properties`
和 `player.properties`（宝箱时间、背包、形象、预设）。同一时间只能运行一个实例。

## 代码结构

| 包 | 内容 |
|---|---|
| `com.wwtsr.pet` | 桌宠本身，不含界面：身体状态（状态模式）、眨眼、敲击计数、监听器 |
| `com.wwtsr.input` | `InputSource`（按键从哪里来）及其背后的 Windows 钩子 |
| `com.wwtsr.stats` | 敲击计数，以及 `TapStore`（两次运行之间把它们存在哪里） |
| `com.wwtsr.collect` | 收集物（`Role`、`Accessory`、`Emote`）、目录 `Catalog`、背包（`Inventory`）、形象（`Loadout`）、合成 `Synthesis` |
| `com.wwtsr.chest` | 宝箱位，以及宝箱开出什么 |
| `com.wwtsr.player` | `Player`：宝箱、背包、形象和预设，以及围绕它们的全部操作；`PlayerStore` |
| `com.wwtsr.settings` | `Settings`（不可变）、`AppSettings`（当前生效的那份，带监听器）、`SettingsStore` |
| `com.wwtsr.menu` | 不依赖界面库描述的菜单（`MenuEntry`），由 `AppMenu` 生成 |
| `com.wwtsr.panel` | 面板窗口及其各页 |
| `com.wwtsr.view` | 桌宠的画面、计数条、宝箱、表情扇面、拖动和窗口 |
| `com.wwtsr.tray` | 系统托盘图标（AWT）；右键打开的菜单与桌宠上的 JavaFX 菜单相同 |
| `com.wwtsr.i18n` | 应用的中文和英文文字 |
| `com.wwtsr.platform` | 仅限 Windows 的部分，都放在接口后面：窗口样式、全屏检测、登录时启动、单实例 |
| `com.wwtsr.assets` | `AssetPack`（全部图片）和生成的占位图包 |
| `com.wwtsr.io` | 小数据文件的安全读写 |
| `com.wwtsr.app` | 启动、组装和开发工具 |
