# 原始数据集 (Raw Data)

本目录包含小香樟心理助手项目所使用的所有原始训练数据。

## 📊 数据概览

| 数据集 | 来源 | 样本数 | 大小 | 说明 |
|--------|------|--------|------|------|
| CPsyCounD | GitHub | 3,084 | 8.1MB | 心理咨询对话数据 |
| CPsyCounE | GitHub | 45 | 128KB | 心理咨询案例数据 |
| PSYQA | GitHub | 246,386 | 286MB | 心理问答数据 |
| **总计** | - | **249,515** | ~294MB | - |

## 📁 目录结构

```
rawdata/
├── README.md                 # 本文件
├── datasources.json          # 数据来源元信息
├── clean/                    # 清洗后的标准格式数据
│   ├── CPsyCounD.jsonl       # CPsyCoun对话数据
│   ├── CPsyCounE.jsonl       # CPsyCoun案例数据
│   └── PSYQA.jsonl           # 心理问答数据
└── original/                 # 原始数据集
    ├── CPsyCoun-main/        # CPsyCoun完整仓库
    │   ├── CPsyCounD/        # 对话数据
    │   ├── CPsyCounE/        # 案例数据
    │   ├── Code/             # 数据处理代码
    │   └── README.md
    └── Chinese-Psychological-QA-DataSet-master/
        ├── data/             # 问答数据
        └── README.md
```

## 📚 数据来源

### 1. CPsyCoun (心理咨询对话数据集)

**来源**: https://github.com/CAS-SIAT-XinHai/CPsyCoun

**论文**: ACL 2024

**包含**:
- **CPsyCounD**: 3,084 条多轮心理咨询对话
- **CPsyCounE**: 45 条心理咨询案例（9个类别 × 5个案例）

**类别**:
- Career (职业)
- Education (教育)
- Emotion&Stress (情绪与压力)
- Family Relationship (家庭关系)
- Love&Marriage (恋爱与婚姻)
- Mental Disease (心理疾病)
- Self-growth (自我成长)
- Sex (性)
- Social Relationship (社会关系)

### 2. Chinese-Psychological-QA-DataSet (心理问答数据集)

**来源**: https://github.com/flyrae/Chinese-Psychological-QA-DataSet

**包含**: 246,386 条心理健康问答对

**格式**: 单轮问答

## 📄 数据格式

### clean/ 目录下的 JSONL 格式

每行一个 JSON 对象，统一为 ShareGPT 格式：

```json
{
  "messages": [
    {"role": "system", "content": "你是一位专业的心理咨询师..."},
    {"role": "user", "content": "用户问题..."},
    {"role": "assistant", "content": "咨询师回复..."}
  ]
}
```

## 🔄 数据处理流程

```
原始数据 (original/)
    ↓
数据清洗脚本
    ↓
清洗后数据 (clean/)
    ↓
训练数据准备 (各模型 data/ 目录)
```

### 处理脚本位置

- 对话模型数据处理: `dialogue_model/data/clear/`
- 评估模型数据标注: `assessment/labeling/`

## 🛡️ 数据许可

请遵循各原始数据集的许可协议：

- **CPsyCoun**: 请参阅 `original/CPsyCoun-main/LICENSE`
- **Chinese-Psychological-QA-DataSet**: 请参阅原仓库说明

## ⚠️ 注意事项

1. **大文件说明**: 
   - `PSYQA.jsonl` (286MB) 和部分原始数据文件超过 GitHub 限制，未包含在仓库中
   - 请从原始数据源下载：
     - PSYQA: https://github.com/flyrae/Chinese-Psychological-QA-DataSet
     - CPsyCoun: https://github.com/CAS-SIAT-XinHai/CPsyCoun
   - 将下载的数据放置到对应目录即可
2. 数据仅供学术研究和教育目的使用
3. 心理健康数据涉及敏感信息，请谨慎处理
4. 不得将数据用于商业用途或可能对用户造成伤害的应用
5. 使用数据时请引用原始数据集

## 📖 引用

如果使用了这些数据，请引用原始论文：

```bibtex
@inproceedings{cpyscoun2024,
  title={CPsyCoun: A Report-based Multi-turn Dialogue Reconstruction and Evaluation Framework for Chinese Psychological Counseling},
  author={...},
  booktitle={ACL 2024},
  year={2024}
}
```

---

**最后更新**: 2025-12

