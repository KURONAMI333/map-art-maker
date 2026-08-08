# Map Art Maker ロゴ — 確定記録

kura 裁定 2026-08-08。**`mam_B_easel`（イーゼル + 双峰の山）で確定**。

kura のコメント: 「少し内容とはニュアンスが違うが、わかりやすさを優先」。イーゼルは
「画像がバニラの地図になる」という MOD の機能そのものではなく絵画のメタファーだが、
kura はそれを承知の上でわかりやすさを優先して選んでいる。**この選択の重みづけは
kura のものなので、確定後に地図寄りへ寄せる修正・提案はしない。**

## 候補ラウンド

1回目: `mam_A_framed`（ブロック上面の島の絵を額装）/ `mam_B_easel`（イーゼル + 風景画）/
`mam_C_scroll`（両端に軸のある巻物）の3案。統括レビューで `mam_C_scroll` は「軸が
柵/門の支柱に見える」と判定されて候補から除外、`mam_A_framed` と `mam_B_easel` は
「陸の緑が輪郭のない塊」の指摘で修正、変換の瞬間（画像→地図）を主題にした
`mam_D_convert`（写真タイル→矢印→3x3地図タイル）を追加した2回目シートを提示。

kura は2回目のシートから `mam_B_easel` を選択。

## 確定アセット

- `branding/map_art_maker_icon_512/256/128/64.png` — 公開用マスター
  （Modrinth / CurseForge アイコンアップロード用、512 が正）
- `branding/logo_injar_256.png` — in-jar ロゴのステージングコピー（256px）
- `common/src/main/resources/map_art_maker.png` — in-jar ロゴ、配線済み
  （multiloader-loader 規約プラグインの commonResources 経由で NeoForge/Fabric
  両方の jar に入る。mod-062-welcome-board の `common/src/main/resources/
  welcome_board.png` と同じパターンで実証済み）
- `branding/render_map_art_maker_final.py` — 確定版のみを再生成する自己完結
  スクリプト
- `branding/render_map_art_maker_icons.py` — 候補ラウンドの再生成スクリプト
  （A/D には `_NOT_SELECTED`、B には `_CONFIRMED_see_branding_root_for_final`
  を出力ファイル名に付与済み）
- `branding/icon-candidates/` — 全候補 + `_contact_sheet.png`（判断記録として保持）

## 配線

- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` の
  `logoFile="${mod_id}.png"` と `fabric/src/main/resources/fabric.mod.json` の
  `"icon": "${mod_id}.png"` はスキャフォールド生成時から既に存在（変更不要）。
  `${mod_id}` は `gradle.properties` の `mod_id=map_art_maker` を
  `buildSrc/src/main/groovy/multiloader-common.gradle` の `processResources`
  `expand` が解決するので、実ファイル名は `map_art_maker.png` である必要がある
