package com.example.bluecat.service.unified.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bluecat.dto.unified.TestCreateRequest;
import com.example.bluecat.dto.unified.TestDetailDTO;
import com.example.bluecat.dto.unified.TestListDTO;
import com.example.bluecat.dto.unified.TestResultDTO;
import com.example.bluecat.dto.unified.TestSubmissionDTO;
import com.example.bluecat.entity.unified.Option;
import com.example.bluecat.entity.unified.Question;
import com.example.bluecat.entity.unified.Response;
import com.example.bluecat.entity.unified.Result;
import com.example.bluecat.entity.unified.Test;
import com.example.bluecat.entity.unified.TestSession;
import com.example.bluecat.mapper.unified.OptionMapper;
import com.example.bluecat.mapper.unified.QuestionMapper;
import com.example.bluecat.mapper.unified.ResponseMapper;
import com.example.bluecat.mapper.unified.ResultMapper;
import com.example.bluecat.mapper.unified.TestMapper;
import com.example.bluecat.mapper.unified.TestSessionMapper;
import com.example.bluecat.service.unified.UnifiedTestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UnifiedTestServiceImpl implements UnifiedTestService {

    private final TestMapper testMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final TestSessionMapper testSessionMapper;
    private final ResponseMapper responseMapper;
    private final ResultMapper resultMapper;

    public UnifiedTestServiceImpl(TestMapper testMapper,
                                  QuestionMapper questionMapper,
                                  OptionMapper optionMapper,
                                  TestSessionMapper testSessionMapper,
                                  ResponseMapper responseMapper,
                                  ResultMapper resultMapper) {
        this.testMapper = testMapper;
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.testSessionMapper = testSessionMapper;
        this.responseMapper = responseMapper;
        this.resultMapper = resultMapper;
    }

    @Override
    public List<TestListDTO> getActiveTests() {
        QueryWrapper<Test> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_active", true);
        return testMapper.selectList(queryWrapper).stream()
                .map(this::toTestListDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TestDetailDTO getTestDetails(Long testId) {
        Test test = testMapper.selectById(testId);
        if (test == null) return null;

        TestDetailDTO testDetailDTO = toTestDetailDTO(test);

        List<Question> questions = questionMapper.selectList(new QueryWrapper<Question>().eq("test_id", testId).orderByAsc("order_no"));
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());

        Map<Long, List<Option>> optionsMap = optionMapper.selectList(new QueryWrapper<Option>().in("question_id", questionIds).orderByAsc("order_no"))
                .stream().collect(Collectors.groupingBy(Option::getQuestionId));

        List<TestDetailDTO.QuestionDTO> questionDTOs = questions.stream().map(q -> {
            TestDetailDTO.QuestionDTO qDto = toQuestionDTO(q);
            qDto.setOptions(optionsMap.getOrDefault(q.getId(), List.of()).stream().map(this::toOptionDTO).collect(Collectors.toList()));
            return qDto;
        }).collect(Collectors.toList());

        testDetailDTO.setQuestions(questionDTOs);
        return testDetailDTO;
    }

    @Override
    public TestSession createTestSession(Long testId, Long userId) {
        TestSession session = new TestSession();
        session.setTestId(testId);
        session.setUserId(userId);
        session.setStartedAt(LocalDateTime.now());
        session.setStatus("ongoing");
        testSessionMapper.insert(session);
        return session;
    }

    @Override
    @Transactional
    public TestResultDTO submitAnswers(Long sessionId, TestSubmissionDTO submissionDTO) {
        TestSession session = testSessionMapper.selectById(sessionId);
        if (session == null || !"ongoing".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not valid or already finished.");
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        List<Long> optionIds = submissionDTO.getAnswers().stream().map(TestSubmissionDTO.AnswerDTO::getOptionId).collect(Collectors.toList());
        Map<Long, Option> optionsMap = optionMapper.selectBatchIds(optionIds).stream().collect(Collectors.toMap(Option::getId, o -> o));

        for (TestSubmissionDTO.AnswerDTO answer : submissionDTO.getAnswers()) {
            Response response = new Response();
            response.setSessionId(sessionId);
            response.setQuestionId(answer.getQuestionId());
            response.setOptionId(answer.getOptionId());
            responseMapper.insert(response);

            Option selectedOption = optionsMap.get(answer.getOptionId());
            if (selectedOption != null && selectedOption.getScore() != null) {
                totalScore = totalScore.add(selectedOption.getScore());
            }
        }

        session.setStatus("finished");
        session.setFinishedAt(LocalDateTime.now());
        testSessionMapper.updateById(session);

        // 计算等级：读取 tests.total_score_thresholds，格式："0:正常;10:轻度;20:中度;30:重度"
        Test test = testMapper.selectById(session.getTestId());
        String level = calculateLevelByThresholds(totalScore, test != null ? test.getTotalScoreThresholds() : null);

        Result result = new Result();
        result.setSessionId(sessionId);
        result.setTotalScore(totalScore);
        result.setLevel(level);
        result.setCreatedAt(LocalDateTime.now());
        resultMapper.insert(result);
        return toTestResultDTO(result);
    }

    @Override
    public TestResultDTO getTestResult(Long sessionId) {
        Result result = resultMapper.selectOne(new QueryWrapper<Result>().eq("session_id", sessionId));
        return result != null ? toTestResultDTO(result) : null;
    }

    @Override
    public Test createTest(TestCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("问卷编码不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("问卷名称不能为空");
        }
        if (testMapper.selectOne(new QueryWrapper<Test>().eq("code", request.getCode())) != null) {
            throw new IllegalArgumentException("问卷编码已存在: " + request.getCode());
        }
        Test test = new Test();
        test.setCode(request.getCode().trim());
        test.setName(request.getName().trim());
        test.setDescription(request.getDescription());
        test.setCategory(Optional.ofNullable(request.getCategory()).filter(s -> !s.isBlank()).orElse("未分类"));
        test.setVersion(1);
        test.setIsActive(true);
        test.setTotalScoreThresholds(request.getTotalScoreThresholds());
        test.setCreatedAt(LocalDateTime.now());
        test.setUpdatedAt(LocalDateTime.now());
        testMapper.insert(test);
        return test;
    }

    @Override
    @Transactional
    public void importQuestions(Long testId, MultipartFile file) {
        if (testId == null) {
            throw new IllegalArgumentException("testId不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        Test test = testMapper.selectById(testId);
        if (test == null) {
            throw new IllegalArgumentException("问卷不存在");
        }

        Map<Integer, QuestionDraft> questionDrafts = readQuestionDrafts(file);
        if (questionDrafts.isEmpty()) {
            throw new IllegalArgumentException("CSV中未读取到有效题目，请检查格式");
        }

        // 清空旧题目
        List<Long> oldQuestionIds = questionMapper.selectList(new QueryWrapper<Question>().eq("test_id", testId))
                .stream().map(Question::getId).collect(Collectors.toList());
        if (!oldQuestionIds.isEmpty()) {
            optionMapper.delete(new QueryWrapper<Option>().in("question_id", oldQuestionIds));
        }
        questionMapper.delete(new QueryWrapper<Question>().eq("test_id", testId));

        // 重新写入
        questionDrafts.values().forEach(draft -> {
            Question question = new Question();
            question.setTestId(testId);
            question.setStem(draft.stem);
            question.setOrderNo(draft.order);
            question.setType("single");
            questionMapper.insert(question);

            draft.options.stream()
                    .sorted(Comparator.comparingInt(o -> o.order))
                    .forEach(optionDraft -> {
                Option option = new Option();
                option.setQuestionId(question.getId());
                option.setLabel(optionDraft.label);
                option.setValueStr(optionDraft.value);
                option.setScore(optionDraft.score);
                option.setOrderNo(optionDraft.order);
                optionMapper.insert(option);
            });
        });
    }

    @Override
    public byte[] exportResponses(Long testId) {
        if (testId == null) {
            throw new IllegalArgumentException("testId不能为空");
        }
        Test test = testMapper.selectById(testId);
        if (test == null) {
            throw new IllegalArgumentException("问卷不存在");
        }
        List<TestSession> sessions = testSessionMapper.selectList(new QueryWrapper<TestSession>().eq("test_id", testId).orderByDesc("started_at"));
        Map<Long, Result> resultMap = sessions.isEmpty() ? Map.of() :
                resultMapper.selectList(new QueryWrapper<Result>().in("session_id", sessions.stream().map(TestSession::getId).collect(Collectors.toList())))
                        .stream().collect(Collectors.toMap(Result::getSessionId, r -> r));

        List<Question> questions = questionMapper.selectList(new QueryWrapper<Question>().eq("test_id", testId));
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        List<Option> options = questions.isEmpty() ? List.of()
                : optionMapper.selectList(new QueryWrapper<Option>().in("question_id", questions.stream().map(Question::getId).collect(Collectors.toList())));
        Map<Long, Option> optionMap = options.stream().collect(Collectors.toMap(Option::getId, o -> o));

        List<Response> responses = sessions.isEmpty() ? List.of()
                : responseMapper.selectList(new QueryWrapper<Response>().in("session_id", sessions.stream().map(TestSession::getId).collect(Collectors.toList())));
        Map<Long, List<Response>> responseMap = responses.stream().collect(Collectors.groupingBy(Response::getSessionId));

        StringBuilder sb = new StringBuilder();
        sb.append("test_code,test_name,category,session_id,user_id,question_id,question_stem,option_id,option_label,option_score,total_score,level,started_at,finished_at\n");
        if (sessions.isEmpty()) {
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        for (TestSession session : sessions) {
            List<Response> sessionResponses = responseMap.getOrDefault(session.getId(), List.of());
            Result result = resultMap.get(session.getId());
            if (sessionResponses.isEmpty()) {
                appendCsvRow(sb, test, session, null, null, null, result);
            } else {
                for (Response response : sessionResponses) {
                    Question question = questionMap.get(response.getQuestionId());
                    Option option = optionMap.get(response.getOptionId());
                    appendCsvRow(sb, test, session, question, option, response, result);
                }
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public void offlineTest(Long testId) {
        if (testId == null) {
            throw new IllegalArgumentException("testId不能为空");
        }
        Test test = testMapper.selectById(testId);
        if (test == null) {
            throw new IllegalArgumentException("问卷不存在");
        }

        // 删除题目与选项
        List<Question> questions = questionMapper.selectList(new QueryWrapper<Question>().eq("test_id", testId));
        if (!questions.isEmpty()) {
            List<Long> qids = questions.stream().map(Question::getId).collect(Collectors.toList());
            optionMapper.delete(new QueryWrapper<Option>().in("question_id", qids));
            questionMapper.delete(new QueryWrapper<Question>().eq("test_id", testId));
        }

        // 删除会话、响应、结果
        List<TestSession> sessions = testSessionMapper.selectList(new QueryWrapper<TestSession>().eq("test_id", testId));
        if (!sessions.isEmpty()) {
            List<Long> sids = sessions.stream().map(TestSession::getId).collect(Collectors.toList());
            responseMapper.delete(new QueryWrapper<Response>().in("session_id", sids));
            resultMapper.delete(new QueryWrapper<Result>().in("session_id", sids));
            testSessionMapper.delete(new QueryWrapper<TestSession>().eq("test_id", testId));
        }

        // 删除问卷本身
        testMapper.deleteById(testId);
    }

    private Map<Integer, QuestionDraft> readQuestionDrafts(MultipartFile file) {
        Map<Integer, QuestionDraft> drafts = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!headerSkipped && line.toLowerCase().contains("question")) {
                    headerSkipped = true;
                    continue;
                }
                List<String> cells = parseCsvLine(line);
                if (cells.size() < 4) {
                    continue;
                }
                int questionOrder = parseIntSafe(getCell(cells, 0), drafts.size() + 1);
                String stem = getCell(cells, 1);
                if (stem.isEmpty()) {
                    continue;
                }
                int optionOrder;
                String optionLabel;
                BigDecimal optionScore;
                if (cells.size() >= 5) {
                    optionOrder = parseIntSafe(getCell(cells, 2), 1);
                    optionLabel = getCell(cells, 3);
                    optionScore = parseDecimal(getCell(cells, 4));
                } else {
                    optionOrder = drafts.containsKey(questionOrder) ? drafts.get(questionOrder).options.size() + 1 : 1;
                    optionLabel = getCell(cells, 2);
                    optionScore = parseDecimal(getCell(cells, 3));
                }
                if (optionLabel.isEmpty()) {
                    continue;
                }
                QuestionDraft draft = drafts.computeIfAbsent(questionOrder, order -> new QuestionDraft(order, stem));
                draft.stem = stem;
                draft.options.add(new OptionDraft(optionOrder, optionLabel, optionLabel, optionScore));
            }
        } catch (IOException e) {
            throw new RuntimeException("解析CSV失败: " + e.getMessage(), e);
        }
        return drafts;
    }

    private void appendCsvRow(StringBuilder sb, Test test, TestSession session,
                              Question question, Option option, Response response, Result result) {
        sb.append(escapeCsv(test.getCode())).append(',')
                .append(escapeCsv(test.getName())).append(',')
                .append(escapeCsv(test.getCategory())).append(',')
                .append(session.getId()).append(',')
                .append(session.getUserId()).append(',');

        if (question != null) {
            sb.append(question.getId()).append(',')
                    .append(escapeCsv(question.getStem())).append(',');
        } else {
            sb.append(',').append(',');
        }

        if (option != null) {
            sb.append(option.getId()).append(',')
                    .append(escapeCsv(option.getLabel())).append(',')
                    .append(option.getScore() != null ? option.getScore().toPlainString() : "").append(',');
        } else {
            sb.append(',').append(',').append(','); // option_id, option_label, option_score
        }

        sb.append(result != null && result.getTotalScore() != null ? result.getTotalScore().toPlainString() : "").append(',')
                .append(result != null ? escapeCsv(result.getLevel()) : "").append(',')
                .append(session.getStartedAt() != null ? escapeCsv(session.getStartedAt()) : "").append(',')
                .append(session.getFinishedAt() != null ? escapeCsv(session.getFinishedAt()) : "")
                .append('\n');
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString().trim());
        return cells;
    }

    private String getCell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        return Objects.toString(cells.get(index), "").trim();
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String str = String.valueOf(value);
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private static class QuestionDraft {
        private final int order;
        private String stem;
        private final List<OptionDraft> options = new ArrayList<>();

        private QuestionDraft(int order, String stem) {
            this.order = order;
            this.stem = stem;
        }
    }

    private static class OptionDraft {
        private final int order;
        private final String label;
        private final String value;
        private final BigDecimal score;

        private OptionDraft(int order, String label, String value, BigDecimal score) {
            this.order = order;
            this.label = label;
            this.value = value;
            this.score = score;
        }
    }

    // --- Mappers ---
    private TestListDTO toTestListDTO(Test test) {
        TestListDTO dto = new TestListDTO();
        dto.setId(test.getId());
        dto.setCode(test.getCode());
        dto.setName(test.getName());
        dto.setDescription(test.getDescription());
        dto.setCategory(test.getCategory());
        return dto;
    }

    private TestDetailDTO toTestDetailDTO(Test test) {
        TestDetailDTO dto = new TestDetailDTO();
        dto.setId(test.getId());
        dto.setCode(test.getCode());
        dto.setName(test.getName());
        dto.setDescription(test.getDescription());
        dto.setCategory(test.getCategory());
        return dto;
    }

    private TestDetailDTO.QuestionDTO toQuestionDTO(Question question) {
        TestDetailDTO.QuestionDTO dto = new TestDetailDTO.QuestionDTO();
        dto.setId(question.getId());
        dto.setStem(question.getStem());
        dto.setType(question.getType());
        dto.setOrderNo(question.getOrderNo());
        return dto;
    }

    private TestDetailDTO.OptionDTO toOptionDTO(Option option) {
        TestDetailDTO.OptionDTO dto = new TestDetailDTO.OptionDTO();
        dto.setId(option.getId());
        dto.setLabel(option.getLabel());
        dto.setOrderNo(option.getOrderNo());
        return dto;
    }

    private TestResultDTO toTestResultDTO(Result result) {
        TestResultDTO dto = new TestResultDTO();
        dto.setSessionId(result.getSessionId());
        dto.setTotalScore(result.getTotalScore());
        dto.setLevel(result.getLevel());
        dto.setResultJson(result.getResultJson());
        return dto;
    }

    private String calculateLevelByThresholds(BigDecimal totalScore, String thresholds) {
        if (totalScore == null) {
            return null;
        }
        if (thresholds == null || thresholds.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = thresholds.split(";");
            java.util.List<AbstractMap.SimpleEntry<BigDecimal, String>> list = new java.util.ArrayList<>();
            for (String p : parts) {
                String s = p.trim();
                if (s.isEmpty() || !s.contains(":")) {
                    continue;
                }
                String[] kv = s.split(":", 2);
                BigDecimal min = new BigDecimal(kv[0].trim());
                String label = kv[1].trim();
                list.add(new AbstractMap.SimpleEntry<>(min, label));
            }
            list.sort(Comparator.comparing(AbstractMap.SimpleEntry::getKey));
            String level = null;
            for (AbstractMap.SimpleEntry<BigDecimal, String> entry : list) {
                if (totalScore.compareTo(entry.getKey()) >= 0) {
                    level = entry.getValue();
                } else {
                    break;
                }
            }
            return level;
        } catch (Exception ignore) {
            return null;
        }
    }
}
