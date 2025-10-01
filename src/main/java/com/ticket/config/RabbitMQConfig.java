package com.ticket.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // 消息轉換器 - 必須！
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // 配置 RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    
    // 配置監聽器工廠
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);  // 併發消費者數量
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
    
    // 交換機
    @Bean
    public DirectExchange ticketExchange() {
        return new DirectExchange("ticket.purchase.exchange", true, false);
    }
    
    // 隊列
    @Bean
    public Queue ticketQueue() {
        return new Queue("ticket.purchase.queue", true, false, false);
    }
    
    // 綁定
    @Bean
    public Binding binding(Queue ticketQueue, DirectExchange ticketExchange) {
        return BindingBuilder.bind(ticketQueue)
                .to(ticketExchange)
                .with("ticket.purchase");
    }
}