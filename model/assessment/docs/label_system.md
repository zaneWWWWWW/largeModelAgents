# 学生心理评估标签体系规范

## 目标
- 面向学生人群的心理健康评估，兼顾可解释性、可自评性与微调效果。
- 输出结构化结果，用于模型训练与产品呈现。

## 字段与取值
- 核心维度
  - `depression_level`: 0–3（无/轻度/中度/重度）
  - `anxiety_level`: 0–3（无/轻度/中度/重度）
  - `functional_impairment`: 0–3（学习/社交/睡眠受影响程度）
  - `risk_flag`: none | suicidal | self_harm | violence | psychosis | mania
  - `risk_severity`: 0–3（风险严重度）
- 学生特化维度
  - `academic_stress`: 0–3（作业、考试、绩点、毕业/升学相关压力）
  - `relationship_stress`: 0–3（同学、室友、恋爱、家庭困扰）
  - `sleep_quality`: 0–3（0好→3差；失眠、入睡困难、早醒）
  - `somatic_symptoms`: 0–3（头痛、心悸、胃痛等）
  - `self_efficacy`: 0–3（0高→3低）
  - `protective_factors`: [social_support, coping_skills, physical_activity, campus_services]
  - `trend`: improving | stable | worsening
  - `duration`: acute | subacute | chronic（<2周、2–6周、>6周）
- 面向用户的简易自评
  - `student_distress_score`: 0–9（综合自评分）
  - 评分映射示例（可校准）：焦虑 0.25、抑郁 0.35、睡眠 0.2、功能受损 0.2，经标准化融合至 0–9。
- 证据与建议
  - `evidence`: [string]（如“近两周失眠”“考试压力显著”）
  - `recommendations`: [string]（分层可执行建议：习惯/学业/心理/求助）
  - `confidence`: 0–1（可选，用于后验校准）

## 等级判定参考
- 抑郁/焦虑 0–3 判定参考
  - 0：无显著症状或偶发，功能基本不受影响
  - 1：轻度症状，持续时间短，轻微功能受损
  - 2：中度症状，持续≥2周，明确功能受损（学习、社交、睡眠）
  - 3：重度症状，显著功能受损或伴风险旗标
- 功能受损 0–3
  - 学习效率下降、考前无法复习、社交退缩、睡眠质量显著下降等作为量化依据
- 风险旗标与严重度
  - 存在自伤/他伤/精神病性症状/躁狂冲动等标记；严重度按紧急性与危害性评估

## JSON 输出结构示例
```json
{
  "depression_level": 1,
  "anxiety_level": 2,
  "functional_impairment": 1,
  "risk_flag": "none",
  "risk_severity": 0,
  "academic_stress": 2,
  "relationship_stress": 1,
  "sleep_quality": 2,
  "somatic_symptoms": 1,
  "self_efficacy": 2,
  "protective_factors": ["social_support"],
  "trend": "worsening",
  "duration": "subacute",
  "student_distress_score": 5,
  "evidence": ["考试临近、入睡困难", "学习效率下降"],
  "recommendations": [
    "规律作息与睡眠卫生",
    "任务分解与时间管理",
    "正念练习10分钟",
    "尝试校园心理咨询预约"
  ],
  "confidence": 0.76
}
```

## 标注流程建议
- 分阶段标注
  - 第一步：证据抽取（症状、时间线、功能影响、诱因）；输出 `evidence`
  - 第二步：核心维度（抑郁/焦虑/功能受损/风险）独立任务标注
  - 第三步：学生特化维度（学业/人际/睡眠/躯体化/自我效能等）
  - 第四步：聚合一致性检查与冲突修复（如“睡眠=0”但证据含“失眠”）
  - 第五步：生成 `student_distress_score` 与分层建议
- 结构化约束
  - 强制枚举/范围校验与必填字段；无证据时输出“未知/不适用”而非臆断
- 稳健性提升
  - 自一致性与投票：提示变体/多次运行多数投票或置信度加权
  - 小规模金标：每类等级抽样人工精标用于阈值与提示校准

## 评估指标
- 多标签一致性：宏/微 F1、Hamming、Quadratic Weighted Kappa（严重度）
- 解释质量：证据覆盖率与证据-结论一致性
- 校准：ECE/分箱校准，输出 `confidence` 做后验校准
- 切片评估：按主题与对话长度分层（参考 CPsyCounE 九类）

## 训练与产品对接
- 训练样本：在归一化数据的 `conversation` 基础上添加 `labels` 字段（上述结构）
- 学生端呈现：显示 `student_distress_score` 与 3–5 条动手建议
- 专业端呈现：完整标签 JSON 与证据列表，便于复核与跟进