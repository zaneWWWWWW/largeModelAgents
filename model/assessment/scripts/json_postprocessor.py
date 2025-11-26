#!/usr/bin/env python3
"""
Robust JSON后处理模块
用于从模型输出中提取和修复JSON格式
"""

import json
import re
from typing import Optional, Dict, Any


class JSONPostProcessor:
    """JSON后处理器，用于提取和修复模型输出中的JSON"""
    
    def __init__(self):
        self.required_fields = ["depression_level", "anxiety_level", "risk_flag", "student_distress_score"]
        self.valid_risk_flags = ["none", "suicidal", "self_harm", "violence"]
    
    def _quick_fix(self, json_str: str) -> str:
        """快速修复常见的JSON格式问题"""
        fixed = json_str
        # 修复字段名中的前导空格
        fixed = re.sub(r'"\s+anxiety[_\s]*level"', r'"anxiety_level"', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"\s+depression[_\s]*level"', r'"depression_level"', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"\s+risk[_\s]*flag"', r'"risk_flag"', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"\s+student[_\s]*distress[_\s]*score"', r'"student_distress_score"', fixed, flags=re.IGNORECASE)
        return fixed
    
    def _complete_json(self, json_str: str) -> Optional[str]:
        """尝试补全不完整的JSON"""
        if not json_str.startswith('{'):
            return None
        
        # 如果JSON不完整（没有闭合），尝试补全
        if not json_str.rstrip().endswith('}'):
            # 提取已有的字段
            result = {}
            
            # 提取已有的字段值
            for field in self.required_fields:
                pattern = f'"{field}"[\\s]*:[\\s]*([^,}}]+)'
                match = re.search(pattern, json_str, re.IGNORECASE)
                if match:
                    value = match.group(1).strip().strip('"\'')
                    if field == "risk_flag":
                        result[field] = value if value in self.valid_risk_flags else "none"
                    else:
                        try:
                            result[field] = int(value)
                        except:
                            result[field] = 0
            
            # 填充缺失字段
            if "depression_level" not in result:
                result["depression_level"] = 0
            if "anxiety_level" not in result:
                result["anxiety_level"] = 0
            if "risk_flag" not in result:
                result["risk_flag"] = "none"
            if "student_distress_score" not in result:
                result["student_distress_score"] = 0
            
            return json.dumps(result, ensure_ascii=False)
        
        return None
    
    def extract_json(self, text: str) -> Optional[str]:
        """
        从文本中提取JSON字符串
        支持多种格式和不完整的情况
        """
        if not text or not isinstance(text, str):
            return None
        
        # 方法1: 尝试找到完整的JSON对象（支持嵌套和多行）
        json_patterns = [
            r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}',  # 标准JSON对象
            r'\{[^}]*\}',  # 简单JSON对象（可能不完整）
        ]
        
        for pattern in json_patterns:
            matches = re.finditer(pattern, text, re.DOTALL)
            for match in matches:
                json_str = match.group(0)
                # 先尝试修复常见问题再解析
                json_str_fixed = self._quick_fix(json_str)
                try:
                    json.loads(json_str_fixed)
                    return json_str_fixed
                except:
                    # 如果修复后还是不行，尝试原始字符串
                    try:
                        json.loads(json_str)
                        return json_str
                    except:
                        # 如果还是不完整，尝试补全
                        completed = self._complete_json(json_str)
                        if completed:
                            try:
                                json.loads(completed)
                                return completed
                            except:
                                continue
                        continue
        
        # 方法2: 尝试补全不完整的JSON
        incomplete_match = re.search(r'\{[^}]*$', text, re.DOTALL)
        if incomplete_match:
            completed = self._complete_json(incomplete_match.group(0))
            if completed:
                return completed
        
        # 方法3: 如果找不到完整JSON，尝试从文本中提取字段
        return self._extract_fields_from_text(text)
    
    def _extract_fields_from_text(self, text: str) -> Optional[str]:
        """从非JSON格式的文本中提取字段"""
        result = {}
        
        # 提取depression_level（支持多种格式，包括括号格式）
        dep_patterns = [
            r'depression[_\s]*level[:\s(]*(\d)',  # 支持括号格式: depression_level(0-3)
            r'depression[_\s]*level[:\s]*["\']?(\d)["\']?',
            r'"depression_level"[:\s]*["\']?(\d)["\']?',
        ]
        for pattern in dep_patterns:
            dep_match = re.search(pattern, text, re.IGNORECASE)
            if dep_match:
                try:
                    result["depression_level"] = int(dep_match.group(1))
                    break
                except:
                    continue
        
        # 提取anxiety_level（处理可能的空格，支持括号格式）
        anx_patterns = [
            r'anxiety[_\s]*level[:\s(]*(\d)',  # 支持括号格式
            r'anxiety[_\s]*level[:\s]*["\']?(\d)["\']?',
            r'"anxiety_level"[:\s]*["\']?(\d)["\']?',
            r'"\s*anxiety[_\s]*level"[:\s]*["\']?(\d)["\']?',
        ]
        for pattern in anx_patterns:
            anx_match = re.search(pattern, text, re.IGNORECASE)
            if anx_match:
                try:
                    result["anxiety_level"] = int(anx_match.group(1))
                    break
                except:
                    continue
        
        # 提取risk_flag
        risk_patterns = [
            (r'risk[_\s]*flag[:\s]*["\']?(none|suicidal|self[_\s-]?harm|violence)["\']?', re.IGNORECASE),
            (r'风险[_\s]*标志?[:\s]*["\']?(无|自杀|自伤|暴力)["\']?', re.IGNORECASE),
            (r'风险[_\s]*flag[:\s]*["\']?(无|自杀|自伤|暴力)["\']?', re.IGNORECASE),
        ]
        risk_mapping = {
            "无": "none", "none": "none",
            "自杀": "suicidal", "suicidal": "suicidal",
            "自伤": "self_harm", "self_harm": "self_harm", "self-harm": "self_harm",
            "暴力": "violence", "violence": "violence"
        }
        
        for pattern, flags in risk_patterns:
            risk_match = re.search(pattern, text, flags)
            if risk_match:
                risk_value = risk_match.group(1).lower().replace("-", "_")
                result["risk_flag"] = risk_mapping.get(risk_value, "none")
                break
        
        # 提取student_distress_score（支持括号格式）
        score_patterns = [
            r'student[_\s]*distress[_\s]*score[:\s(]*(\d)',  # 支持括号格式
            r'"student_distress_score"[:\s]*["\']?(\d)["\']?',
            r'student[_\s]*distress[_\s]*score[:\s]*["\']?(\d)["\']?',
        ]
        for pattern in score_patterns:
            score_match = re.search(pattern, text, re.IGNORECASE)
            if score_match:
                try:
                    result["student_distress_score"] = int(score_match.group(1))
                    break
                except:
                    continue
        
        # 如果至少提取到一个字段，返回JSON字符串
        if result:
            # 填充缺失字段为默认值
            if "depression_level" not in result:
                result["depression_level"] = 0
            if "anxiety_level" not in result:
                result["anxiety_level"] = 0
            if "risk_flag" not in result:
                result["risk_flag"] = "none"
            if "student_distress_score" not in result:
                result["student_distress_score"] = 0
            
            return json.dumps(result, ensure_ascii=False)
        
        return None
    
    def fix_json(self, json_str: str) -> Optional[str]:
        """
        修复JSON字符串中的常见错误
        """
        if not json_str:
            return None
        
        try:
            # 尝试直接解析
            result = json.loads(json_str)
            return json.dumps(result, ensure_ascii=False)
        except json.JSONDecodeError:
            pass
        
        # 修复常见问题
        fixed = json_str
        
        # 1. 修复字段名中的空格和大小写问题（更全面的匹配）
        # 处理 " Anxiety_level" 这种情况（前面有空格）
        fixed = re.sub(r'"\s+([Aa]nxiety[_\s]*[Ll]evel)\s*":', r'"anxiety_level":', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"\s+([Dd]epression[_\s]*[Ll]evel)\s*":', r'"depression_level":', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"\s+([Rr]isk[_\s]*[Ff]lag)\s*":', r'"risk_flag":', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"\s+([Ss]tudent[_\s]*[Dd]istress[_\s]*[Ss]core)\s*":', r'"student_distress_score":', fixed, flags=re.IGNORECASE)
        
        # 处理字段名中的下划线和空格混合
        fixed = re.sub(r'"anxiety\s+level"', r'"anxiety_level"', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"depression\s+level"', r'"depression_level"', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"risk\s+flag"', r'"risk_flag"', fixed, flags=re.IGNORECASE)
        fixed = re.sub(r'"student\s+distress\s+score"', r'"student_distress_score"', fixed, flags=re.IGNORECASE)
        
        # 2. 修复值类型问题
        # risk_flag应该是字符串，但可能是数字0
        fixed = re.sub(r'"risk_flag":\s*0\s*([,}])', r'"risk_flag":"none"\1', fixed)
        fixed = re.sub(r'"risk_flag":\s*"0"\s*([,}])', r'"risk_flag":"none"\1', fixed)
        
        # 3. 修复数字值（确保在有效范围内）
        # depression_level和anxiety_level应该是0-3
        def fix_level(match):
            field = match.group(1)
            value = int(match.group(2)) if match.group(2).isdigit() else 0
            value = max(0, min(3, value))  # 限制在0-3
            return f'"{field}": {value}'
        
        fixed = re.sub(r'"depression_level":\s*(\d+)', fix_level, fixed)
        fixed = re.sub(r'"anxiety_level":\s*(\d+)', fix_level, fixed)
        
        # student_distress_score应该是0-9
        def fix_score(match):
            value = int(match.group(1)) if match.group(1).isdigit() else 0
            value = max(0, min(9, value))  # 限制在0-9
            return f'"student_distress_score": {value}'
        
        fixed = re.sub(r'"student_distress_score":\s*(\d+)', fix_score, fixed)
        
        # 4. 修复risk_flag的值
        def fix_risk_flag(match):
            value = match.group(1).strip('"\'')
            value_lower = value.lower().replace("-", "_")
            valid_values = {
                "none": "none",
                "suicidal": "suicidal",
                "self_harm": "self_harm",
                "self-harm": "self_harm",
                "violence": "violence",
                "0": "none",
                "无": "none",
                "自杀": "suicidal",
                "自伤": "self_harm",
                "暴力": "violence"
            }
            return f'"risk_flag": "{valid_values.get(value_lower, "none")}"'
        
        fixed = re.sub(r'"risk_flag":\s*["\']?([^,"}]+)["\']?', fix_risk_flag, fixed)
        
        # 5. 清理多余的空白字符
        fixed = re.sub(r'\s+', ' ', fixed)  # 多个空格合并为一个
        fixed = fixed.replace(' ,', ',').replace(', ', ',')
        fixed = fixed.replace(' :', ':').replace(': ', ':')
        
        # 6. 尝试解析修复后的JSON
        try:
            result = json.loads(fixed)
            return json.dumps(result, ensure_ascii=False)
        except json.JSONDecodeError:
            # 如果还是无法解析，尝试更激进的修复
            return self._aggressive_fix(fixed)
    
    def _aggressive_fix(self, text: str) -> Optional[str]:
        """更激进的修复方法"""
        result = {}
        
        # 提取所有可能的字段值
        patterns = {
            "depression_level": [
                r'depression[_\s]*level[:\s]*["\']?(\d)["\']?',
                r'"depression_level"[:\s]*["\']?(\d)["\']?',
            ],
            "anxiety_level": [
                r'anxiety[_\s]*level[:\s]*["\']?(\d)["\']?',
                r'"anxiety_level"[:\s]*["\']?(\d)["\']?',
                r'"\s*Anxiety_level"[:\s]*["\']?(\d)["\']?',
            ],
            "risk_flag": [
                r'risk[_\s]*flag[:\s]*["\']?(none|suicidal|self[_\s-]?harm|violence|0)["\']?',
                r'"risk_flag"[:\s]*["\']?(none|suicidal|self[_\s-]?harm|violence|0)["\']?',
            ],
            "student_distress_score": [
                r'student[_\s]*distress[_\s]*score[:\s]*["\']?(\d)["\']?',
                r'"student_distress_score"[:\s]*["\']?(\d)["\']?',
            ]
        }
        
        risk_mapping = {
            "none": "none", "0": "none",
            "suicidal": "suicidal",
            "self_harm": "self_harm", "self-harm": "self_harm",
            "violence": "violence"
        }
        
        for field, field_patterns in patterns.items():
            for pattern in field_patterns:
                match = re.search(pattern, text, re.IGNORECASE)
                if match:
                    value = match.group(1)
                    if field == "risk_flag":
                        result[field] = risk_mapping.get(value.lower().replace("-", "_"), "none")
                    else:
                        try:
                            int_value = int(value)
                            if field in ["depression_level", "anxiety_level"]:
                                result[field] = max(0, min(3, int_value))
                            elif field == "student_distress_score":
                                result[field] = max(0, min(9, int_value))
                        except:
                            pass
                    break
        
        # 填充缺失字段
        if "depression_level" not in result:
            result["depression_level"] = 0
        if "anxiety_level" not in result:
            result["anxiety_level"] = 0
        if "risk_flag" not in result:
            result["risk_flag"] = "none"
        if "student_distress_score" not in result:
            result["student_distress_score"] = 0
        
        return json.dumps(result, ensure_ascii=False)
    
    def validate_and_fix(self, text: str) -> tuple[Optional[Dict[str, Any]], Optional[str]]:
        """
        完整的验证和修复流程
        返回: (修复后的JSON对象, 错误信息)
        """
        if not text:
            return None, "输入为空"
        
        # 步骤1: 提取JSON字符串
        json_str = self.extract_json(text)
        if not json_str:
            return None, "无法从输出中提取JSON格式"
        
        # 步骤2: 修复JSON
        fixed_json_str = self.fix_json(json_str)
        if not fixed_json_str:
            return None, "无法修复JSON格式"
        
        # 步骤3: 解析和验证
        try:
            result = json.loads(fixed_json_str)
        except json.JSONDecodeError as e:
            return None, f"JSON解析失败: {e}"
        
        # 步骤4: 验证必需字段
        missing_fields = [f for f in self.required_fields if f not in result]
        if missing_fields:
            return None, f"缺少必需字段: {missing_fields}"
        
        # 步骤5: 验证字段值
        errors = []
        
        # 验证depression_level
        if not isinstance(result["depression_level"], int):
            try:
                result["depression_level"] = int(result["depression_level"])
            except:
                errors.append("depression_level必须是整数")
        if not (0 <= result["depression_level"] <= 3):
            result["depression_level"] = max(0, min(3, result["depression_level"]))
            errors.append("depression_level超出范围，已修正")
        
        # 验证anxiety_level
        if not isinstance(result["anxiety_level"], int):
            try:
                result["anxiety_level"] = int(result["anxiety_level"])
            except:
                errors.append("anxiety_level必须是整数")
        if not (0 <= result["anxiety_level"] <= 3):
            result["anxiety_level"] = max(0, min(3, result["anxiety_level"]))
            errors.append("anxiety_level超出范围，已修正")
        
        # 验证risk_flag
        if not isinstance(result["risk_flag"], str):
            result["risk_flag"] = str(result["risk_flag"]).lower()
        result["risk_flag"] = result["risk_flag"].lower().replace("-", "_")
        if result["risk_flag"] not in self.valid_risk_flags:
            result["risk_flag"] = "none"
            errors.append("risk_flag无效，已设置为none")
        
        # 验证student_distress_score
        if not isinstance(result["student_distress_score"], int):
            try:
                result["student_distress_score"] = int(result["student_distress_score"])
            except:
                errors.append("student_distress_score必须是整数")
        if not (0 <= result["student_distress_score"] <= 9):
            result["student_distress_score"] = max(0, min(9, result["student_distress_score"]))
            errors.append("student_distress_score超出范围，已修正")
        
        error_msg = "; ".join(errors) if errors else None
        
        return result, error_msg


def test_postprocessor():
    """测试后处理器"""
    processor = JSONPostProcessor()
    
    test_cases = [
        # 标准JSON
        '{"depression_level": 1, "anxiety_level": 2, "risk_flag": "none", "student_distress_score": 5}',
        # 字段名有空格
        '{"depression_level": 0, " Anxiety_level": 0, " risk_flag": 0, "student_distress_score": 0}',
        # risk_flag是数字
        '{"depression_level": 1, "anxiety_level": 2, "risk_flag": 0, "student_distress_score": 6}',
        # 不完整JSON
        '{"depression_level": 1, "anxiety_level": 2',
        # 非JSON格式
        'depression_level(0-3)',
        # 中文格式
        '风险_flag:自杀',
        # 混合格式
        'depression_level: 2, anxiety_level: 1, risk_flag: "none", student_distress_score: 5',
    ]
    
    print("测试JSON后处理器:")
    print("=" * 60)
    
    for i, test_case in enumerate(test_cases, 1):
        print(f"\n测试用例 {i}: {test_case[:50]}...")
        result, error = processor.validate_and_fix(test_case)
        if result:
            print(f"✓ 成功提取: {json.dumps(result, ensure_ascii=False)}")
            if error:
                print(f"  警告: {error}")
        else:
            print(f"✗ 失败: {error}")


if __name__ == "__main__":
    test_postprocessor()

