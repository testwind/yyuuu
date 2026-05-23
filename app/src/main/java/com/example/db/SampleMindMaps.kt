package com.example.db

object SampleMindMaps {

    val PROJECT_PLAN_XML = """
        <map version="freeplane 1.9.12">
            <node TEXT="🚀 移动App开发项目规划" ID="root_project_plan" COLOR="#1A73E8">
                <node TEXT="📋 1. 需求分析与规划" ID="req_planning" POSITION="right" COLOR="#E11D48">
                    <node TEXT="🎯 确立核心愿景 &amp; MVP范围" ID="vision">
                        <richcontent TYPE="NOTE">
                          <html><body><p>MVP 范围：包含 Freeplane 兼容解析器、缩放平移交互画布、节点折叠功能、以及 WebDAV/ Pairing 跨平台同步。</p></body></html>
                        </richcontent>
                    </node>
                    <node TEXT="👥 用户画像分析" ID="user_persona"/>
                    <node TEXT="🎨 UI/UX 交互设计原型" ID="ui_design" FOLDED="true">
                        <node TEXT="信息流看板" ID="board_p"/>
                        <node TEXT="思维导图绘制区" ID="map_p"/>
                        <node TEXT="跨端同步配置面" ID="sync_p"/>
                    </node>
                </node>
                <node TEXT="💻 2. 架构设计与研发" ID="dev_arch" POSITION="right" COLOR="#D97706">
                    <node TEXT="🏗️ 分层开发架构 (MVVM)" ID="mvvm_arch">
                        <richcontent TYPE="NOTE">
                          <html><body><p>数据层：Room Database 持续本地存储机制；<br/>解析层：XmlPullParser 高效处理 .mm 文件；<br/>UI 渲染层：Jetpack Compose 极速绘制轻量画布。</p></body></html>
                        </richcontent>
                    </node>
                    <node TEXT="⚙️ 跨端同步机制" ID="sync_mechanism">
                        <node TEXT="📡 WebDAV 云同步接口" ID="webdav_impl"/>
                        <node TEXT="🔗 设备即时配对 (多端流转)" ID="wifi_pairing"/>
                    </node>
                    <node TEXT="🔍 渲染画布核心库" ID="draw_engine" FOLDED="true">
                        <node TEXT="贝塞尔曲线分支连接线" ID="bezier_lines"/>
                        <node TEXT="高级多点缩放与无界平移拖拽" ID="scale_pan"/>
                        <node TEXT="单项节点自适应高度折叠布局" ID="folding"/>
                    </node>
                </node>
                <node TEXT="🛡️ 3. 测试与持续质量" ID="testing" POSITION="left" COLOR="#059669">
                    <node TEXT="🧪 Robolectric 单元测试" ID="unit_tests"/>
                    <node TEXT="📸 Roborazzi UI 截图还原测试" ID="screenshot_tests"/>
                    <node TEXT="📱 弱网环境多设备同步鲁棒性" ID="sync_stability_test"/>
                </node>
                <node TEXT="📈 4. 推广与发布上线" ID="deployment" POSITION="left" COLOR="#7C3AED">
                    <node TEXT="🎨 制作应用配套 App Icon 与自适应包" ID="app_icon_flow"/>
                    <node TEXT="🌐 官网 &amp; 多端同步入口上线" ID="web_portal"/>
                    <node TEXT="🚀 部署发布至应用商店" ID="app_stores"/>
                </node>
            </node>
        </map>
    """.trimIndent()

    val PERSONAL_GROWTH_XML = """
        <map version="freeplane 1.9.12">
            <node TEXT="🌿 2026 个人成长金字塔" ID="root_personal" COLOR="#10B981">
                <node TEXT="📚 知识底座与技能" ID="knowledge" POSITION="right" COLOR="#2563EB">
                    <node TEXT="💻 软件工程强化" ID="engineering">
                        <node TEXT="掌握 Jetpack Compose Canvas 原生高性能构图" ID="compose_canvas"/>
                        <node TEXT="了解 Android 端现代离线优先系统同步技术" ID="offline_first"/>
                    </node>
                    <node TEXT="🗣️ 语言学习与沟通" ID="languages" FOLDED="true">
                        <node TEXT="英语日常口语地道表达 (15分钟/天)" ID="english_daily"/>
                        <node TEXT="阅读10本非虚构类英文原版书籍" ID="english_books"/>
                    </node>
                </node>
                <node TEXT="🏃‍♂️ 健康与体能锻炼" ID="health" POSITION="right" COLOR="#EA580C">
                    <node TEXT="🏃 半程马拉松 (21KM) 常态化训练" ID="marathon"/>
                    <node TEXT="🧘 每周两次核心拉伸与冥想放松" ID="meditation"/>
                    <node TEXT="🥦 控糖少碳，建立科学一日三餐" ID="diet_plan"/>
                </node>
                <node TEXT="💰 资产管理与长远投资" ID="finance" POSITION="left" COLOR="#059669">
                    <node TEXT="📈 学习低波红利指数与定投配置" ID="index_funds"/>
                    <node TEXT="🏠 降低日常固定非必要消费比例至30%" ID="cut_expenses"/>
                </node>
                <node TEXT="🎨 兴趣与多元生活体验" ID="hobbies" POSITION="left" COLOR="#DB2777">
                    <node TEXT="🎸 练习木吉他基础弹唱并录制视频" ID="guitar"/>
                    <node TEXT="🏔️ 去三个非主流著名自然保护区徒步" ID="hiking"/>
                </node>
            </node>
        </map>
    """.trimIndent()

    val STRATEGIC_MEETING_XML = """
        <map version="freeplane 1.9.12">
            <node TEXT="📌 核心骨干业务决策框架" ID="root_decision" COLOR="#3B82F6">
                <node TEXT="🔍 Phase 1: 发现与问题陈述" ID="problem_statement" POSITION="right" COLOR="#DC2626">
                    <node TEXT="📊 数据漏斗归因分析" ID="funnel_analysis"/>
                    <node TEXT="💬 提炼核心痛点反馈" ID="user_feedback"/>
                </node>
                <node TEXT="💡 Phase 2: 头脑风暴与设想" ID="brainstorming" POSITION="right" COLOR="#F59E0B">
                    <node TEXT="⚡ 发挥集体智慧，不做先期筛选" ID="no_filter"/>
                    <node TEXT="✏️ 快速白板思维导图草案" ID="quick_draft"/>
                </node>
                <node TEXT="⚖️ Phase 3: 多维度分析筛选" ID="evaluation" POSITION="left" COLOR="#10B981">
                    <node TEXT="📈 实现成本 vs. 商业价值矩阵" ID="matrix_cost"/>
                    <node TEXT="🛡️ 技术可行性与风险红线评估" ID="tech_risk"/>
                </node>
                <node TEXT="🏁 Phase 4: 定论与责任拆解" ID="action_items" POSITION="left" COLOR="#8B5CF6">
                    <node TEXT="👤 划定核心里程碑负责人" ID="owner_assignment"/>
                    <node TEXT="📅 对齐多平台联动同步上线时间表" ID="timeline_sync"/>
                </node>
            </node>
        </map>
    """.trimIndent()
}
