# 后端 API 服务（FastAPI 骨架）

## 快速运行
```bash
cd develop/services/api
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

访问健康检查：`http://localhost:8000/health`

## 环境变量
复制 `.env.example` 为 `.env` 并按需修改。