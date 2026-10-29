package com.example.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantDemoTest {
    @Test fun urgentAndMedicalRequestsOverrideWellnessTopics() {
        assertEquals(AssistantDemo.Topic.URGENT, AssistantDemo.topic("I have chest pain after exercise"))
        assertEquals(AssistantDemo.Topic.URGENT, AssistantDemo.topic("எனக்கு மூச்சு விட முடியவில்லை"))
        assertEquals(AssistantDemo.Topic.MEDICAL, AssistantDemo.topic("Change my medication and meal plan"))
        assertEquals(AssistantDemo.Topic.MEDICAL, AssistantDemo.topic("மருந்து பற்றி உணவு ஆலோசனை"))
    }
    @Test fun englishAndTamilDemoTopicsAndUnsupportedQuestions() {
        assertEquals(AssistantDemo.Topic.PLAN, AssistantDemo.topic("Plan my day"))
        assertEquals(AssistantDemo.Topic.SLEEP, AssistantDemo.topic("ஓய்வும் தூக்கமும்"))
        assertEquals(AssistantDemo.Topic.FOOD, AssistantDemo.topic("உணவுப் பழக்கங்கள்"))
        assertEquals(AssistantDemo.Topic.MOVE, AssistantDemo.topic("Gentle movement"))
        assertEquals(AssistantDemo.Topic.GENERAL, AssistantDemo.topic("Explain quantum computing"))
        assertEquals(AssistantDemo.Topic.GENERAL, AssistantDemo.topic("你好"))
    }
}
