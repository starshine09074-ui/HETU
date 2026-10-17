package com.ruoyi.project.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ruoyi.project.mapper.HetuMapper;

class HetuControllerScoringTest
{
    private static final Method SCORE_BUG = scoreBugMethod();

    @Test
    void developmentDefectsDeductTheConfiguredRatioOfTaskEstimate() throws Exception
    {
        Map<String, String> cases = Map.of(
                "CRITICAL", "-5.00",
                "HIGH", "-3.00",
                "MEDIUM", "-1.00",
                "LOW", "-0.50");

        for (Map.Entry<String, String> testCase : cases.entrySet())
        {
            Map<String, Object> bug = bug("PRODUCT_DEFECT", testCase.getKey(), "CLOSED");
            scoreBug(bug);
            assertEquals(new BigDecimal(testCase.getValue()), bug.get("scoreHours"), testCase.getKey());
        }
    }

    @Test
    void requirementAndEnvironmentDefectsUseFixedHourDeductions() throws Exception
    {
        Map<String, String> cases = Map.of(
                "CRITICAL", "-5.00",
                "HIGH", "-3.00",
                "MEDIUM", "-1.00",
                "LOW", "0.00");

        for (String cause : new String[] {"REQUIREMENT_CHANGE", "ENVIRONMENT"})
        {
            for (Map.Entry<String, String> testCase : cases.entrySet())
            {
                Map<String, Object> bug = bug(cause, testCase.getKey(), "CLOSED");
                scoreBug(bug);
                assertEquals(new BigDecimal(testCase.getValue()), bug.get("scoreHours"), cause + "/" + testCase.getKey());
            }
        }
    }

    @Test
    void nonDeductibleAndUnclosedDefectsKeepAZeroScore() throws Exception
    {
        for (String cause : new String[] {"NO_DEDUCTION", "DUPLICATE", "NOT_REPRODUCIBLE"})
        {
            Map<String, Object> bug = bug(cause, "CRITICAL", "CLOSED");
            scoreBug(bug);
            assertEquals(new BigDecimal("0.00"), bug.get("scoreHours"), cause);
        }

        Map<String, Object> openBug = bug("PRODUCT_DEFECT", "CRITICAL", "IN_PROGRESS");
        scoreBug(openBug);
        assertEquals(new BigDecimal("0.00"), openBug.get("scoreHours"));
    }

    @Test
    void developmentDefectCannotCloseWithoutAnAssociatedTask() throws Exception
    {
        Map<String, Object> bug = bug("PRODUCT_DEFECT", "CRITICAL", "CLOSED");
        bug.remove("taskId");

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> scoreBug(bug));
        assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    }

    private static Map<String, Object> bug(String cause, String severity, String status)
    {
        return new java.util.HashMap<>(Map.of(
                "rootCause", cause,
                "severity", severity,
                "status", status,
                "taskId", 123L));
    }

    private static void scoreBug(Map<String, Object> bug) throws Exception
    {
        HetuMapper mapper = (HetuMapper) Proxy.newProxyInstance(
                HetuMapper.class.getClassLoader(),
                new Class<?>[] {HetuMapper.class},
                (proxy, method, args) -> "taskEstimateHours".equals(method.getName())
                        ? new BigDecimal("10.00")
                        : null);
        SCORE_BUG.invoke(new HetuController(mapper), bug);
    }

    private static Method scoreBugMethod()
    {
        try
        {
            Method method = HetuController.class.getDeclaredMethod("scoreBug", Map.class);
            method.setAccessible(true);
            return method;
        }
        catch (ReflectiveOperationException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }
}
