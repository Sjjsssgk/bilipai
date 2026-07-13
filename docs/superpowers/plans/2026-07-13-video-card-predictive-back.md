# 视频卡片详情预测返回实施计划

1. 在 `BiliPaiNavMotionPolicyTest` 先加入首页、搜索等卡片来源、设置开关、共享过渡开关和无匹配来源的作用域测试；运行定向测试并确认因策略缺失而失败。
2. 在 `BiliPaiNavMotionPolicy` 加入最小纯函数，判定当前详情是否有匹配的卡片来源并允许跟手返回；运行定向测试确认通过。
3. 在 `AnimationSettingsScreen` 读取现有导航设置并加入“视频详情跟手返回”开关；过渡动画关闭时禁用该项。
4. 在 `AppNavigation` 只为匹配来源卡片的详情传入该策略结果；在 `VideoDetailScreen` 关闭时使用页面级经典返回处理，并保持全屏等本地返回处理优先。
5. 在 `BiliPaiNavDisplayHost` 把手势背景进度改为直接读取，移除帧级 `LaunchedEffect(progress)`；补齐快速返回和取消所需的一次性状态衔接。
6. 运行定向单元测试和 `:app:compileDebugKotlin`，检查差异只涉及本功能；提交并推送 `main`。
