package org.recap.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.recap.PropertyKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;


/**
 * Created by premkb on 12/10/16.
 */
@Component
public class ActivemqRegistrar {

    /**
     * Register ActiveMQ queue service, with Camel context
     * @param camelContext
     * @param defaultBrokerURL
     * @throws JMSException
     */
    @Autowired
    public ActivemqRegistrar(CamelContext camelContext , @Value("${" + PropertyKeyConstants.ACTIVEMQ_BROKER_URL + "}") String defaultBrokerURL) throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(defaultBrokerURL);
        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConnectionFactory(connectionFactory);
        activeMQComponent.setObjectMessageEnabled(true);
        activeMQComponent.setTrustAllPackages(true);
        camelContext.addComponent("scsbactivemq", activeMQComponent);
    }
}
