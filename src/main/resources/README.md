# Destroy Chemistry Addon (dchem) v1.3.0

为 Destroy（NHblock714 分支 1.21.1）增加化学内容的 NeoForge 模组。

- **服务端**：数据包实现（Destroy 的数据驱动化学系统），游戏逻辑完全在服务端。
- **客户端**：内置 Java 客户端加载器 + JEI 插件：
  - 启动/资源重载时把本模组的分子与反应 JSON 灌入 Destroy 的客户端注册表并刷新 JEI，
    所有新反应在 JEI"化学反应"分类页可见、可搜索（含中文名与中文描述，悬停查看）；
  - 自定义 JEI 反查插件：**在 JEI/物品栏中点击物品（U/R 键）即可查到相关反应**
    （点 northstar:salt → 氯化钠结晶；点生猪肉 → 肉类浸解；点腐肉 → 肉浆水解……）。

> **重要：JEI 显示与反查需要把本 jar 也装到客户端 mods 文件夹**（和服务端同一个文件）。
> 不装也不影响游戏玩法（反应照常生效），只是 JEI 看不到、名字是英文键名。

依赖：destroy ≥ 0.4.1、petrolpark ≥ 1.5.0、create ≥ 6.0.10、northstar ≥ 0.6.4。

---

## 一、氯化钠结晶 → northstar:salt（任务1）

反应：**1 mol Na⁺ + 1 mol Cl⁻ → 1 个 northstar:salt**，需要**圆石催化** + **加热**。

- 原料：盐水（brine）混合物。蒸馏塔 + 海水 → 500mB 盐水（含 1M Na⁺ 与 1M Cl⁻）
  + 500mB 蒸馏水（destroy 自带配方 `distillation/brine`）。
- 装置：动力搅拌器 + 盆（Basin）+ 下方烈焰人燃烧室（加热）。
- 操作：盆里放 1 个**圆石**（催化剂，不被消耗）+ 盐水，启动搅拌器并加热。
- 产出：盐结晶掉入盆中（1 桶盐水 ≈ 1 个盐）。
- 逆反应：盐 + 水在盆里搅拌重新溶解成 Na⁺ + Cl⁻（点击盐可在 JEI 查到该反应）。

## 二、碳酸根化学（任务2）

新分子：`dchem:carbonate`（CO₃²⁻）、`dchem:bicarbonate`（HCO₃⁻）、
`dchem:carbonic_acid`（H₂CO₃）。强电解质盐类不再制作分子（溶液中的离子对由
Destroy 自带盐命名系统显示，如"碳酸钠"）。

全部 15 个反应（均满足电荷守恒，JEI 反应页可见，悬停有中文描述）：

| 反应 | 说明 |
|---|---|
| **2OH⁻ + CO₂ → CO₃²⁻ + H₂O** | **碱液吸收二氧化碳（消耗氢氧根）** |
| **CO₃²⁻ + CO₂ + H₂O → 2HCO₃⁻** | **氢氧根耗尽后碳酸根继续吸碳变碳酸氢根** |
| H⁺ + CO₃²⁻ → HCO₃⁻ | 碳酸根质子化 |
| 2H⁺ + CO₃²⁻ → CO₂↑ + H₂O | 碳酸根遇酸放出二氧化碳 |
| H⁺ + HCO₃⁻ → CO₂↑ + H₂O | 碳酸氢根遇酸 |
| CO₂ + H₂O ⇌ H₂CO₃ | 二氧化碳水合（缓慢） |
| H₂CO₃ → CO₂ + H₂O | 碳酸分解 |
| H₂CO₃ ⇌ H⁺ + HCO₃⁻ | 碳酸一级电离/质子化 |
| HCO₃⁻ ⇌ H⁺ + CO₃²⁻ | 碳酸氢根电离/质子化 |
| CO₃²⁻ + H₂O ⇌ HCO₃⁻ + OH⁻ | 碳酸根水解/碳酸氢根中和 |
| **Ca²⁺ + CO₃²⁻ → CaCO₃↓** | **碳酸钙沉淀 → destroy:chalk_dust** |
| Ca²⁺ + CO₃²⁻ →(强热) CO₂↑ + CaO↓ | 碳酸钙煅烧 → destroy:quicklime |
| CO₃²⁻ →(强热) CO₂↑ + O²⁻ | 碳酸根热分解 |

## 三、肉类水解（任务3）

两步反应（**不需要加热、不需要蒸馏水、需要氢氧根催化**）：

1. **肉类浸解**（`meat_dissolution`）：生猪肉/生牛肉（`dchem:raw_meat`）→ **肉浆**
   —— 无需蒸馏水：盆/反应釜中只要有任意液体（如盐水）即可进行，不需要加热；
2. **肉浆水解**（`meat_slurry_hydrolysis`）：肉浆 → **甘油（destroy:glycerol）+ 腐肉**↓
   —— **氢氧根（OH⁻）催化**，常温即可完成：溶液中没有氢氧根（碱性溶液，如生石灰水
   生石灰+水、氢氧化钠溶液）时该反应不会发生。

产物：1 份肉 → 1 个腐肉 + 1 mol 甘油（溶在水中，可倒出使用）。

## 安装

1. **服务端**：`dchem-1.3.0.jar` 放入 `mods/`，重启服务器。
   日志应出现 `Loaded 4 datapack molecule(s); 0 skipped.` 与
   `Loaded 19 datapack reaction(s); 0 skipped.`。
2. **客户端（建议）**：同一个 jar 放入客户端 mods，重启客户端。
   JEI 反应分类页出现全部新反应（悬停显示中文描述），点击任意相关物品可用 U/R 反查。
3. 改动 JSON 后：服务端 `/reload`；客户端 F3+T 重载资源。

## 文件结构

```
META-INF/neoforge.mods.toml
dchem/                         Java 客户端加载器 + JEI 插件（Dist.CLIENT 守卫，服务端不加载）
data/dchem/destroy/molecules|reactions/*.json   （服务端数据包）
assets/dchem/destroy/molecules|reactions/*.json （客户端镜像，供客户端加载器读取）
assets/dchem/lang/*.json                        （中英文名与反应描述）
```

## 开发

```
D:\server\dev\dchem-mod\         gradle 工程（moddev 2.0.74 / NeoForge 21.1.248）
D:\server\dev\dchem\             纯数据包源（JSON 的事实来源，改这里）
构建：gradlew build  →  build/libs/dchem-1.3.0.jar
```
