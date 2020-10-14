package org.recap.camel.statusreconciliation;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.recap.RecapConstants;
import org.recap.RecapCommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by hemalathas on 22/5/17.
 */
@Component
public class StatusReconciliationFtpRouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StatusReconciliationFtpRouteBuilder.class);

    /**
     * Instantiates a new Status reconciliation ftp route builder.
     *
     * @param camelContext         the camel context
     * @param applicationContext   the application context
     * @param ftpUserName          the ftp user name
     * @param ftpKnownHost         the ftp known host
     * @param ftpPrivateKey        the ftp private key
     * @param statusReconciliation the status reconciliation
     */
    @Autowired
    public StatusReconciliationFtpRouteBuilder(CamelContext camelContext, ApplicationContext applicationContext,
                                                        @Value("${ftp.server.userName}") String ftpUserName,
                                                        @Value("${ftp.server.knownHost}") String ftpKnownHost, @Value("${ftp.server.privateKey}") String ftpPrivateKey,
                                                        @Value("${status.reconciliation}") String statusReconciliation){
        try{
            camelContext.addRoutes(new RouteBuilder(){
                @Override
                public void configure() throws Exception {
                    from(RecapConstants.STATUS_RECONCILIATION_REPORT)
                            .routeId(RecapConstants.STATUS_RECONCILIATION_REPORT_ID)
                            .onCompletion().onWhen(header(RecapConstants.FOR).isEqualTo(RecapConstants.STATUS_RECONCILIATION))
                            .log("status reconciliation process finished files generated in ftp")
                            .bean(applicationContext.getBean(StatusReconciliationEmailService.class), RecapConstants.PROCESS_INPUT)
                            .end()
                            .choice()
                                .when(header(RecapConstants.FOR).isEqualTo(RecapConstants.STATUS_RECONCILIATION))
                                    .marshal().bindy(BindyType.Csv, StatusReconciliationCSVRecord.class)
                                    .to(RecapCommonConstants.SFTP + ftpUserName + RecapCommonConstants.AT + statusReconciliation + RecapCommonConstants.PRIVATE_KEY_FILE + ftpPrivateKey + RecapCommonConstants.KNOWN_HOST_FILE + ftpKnownHost + "&fileName=StatusReconciliation-${date:now:yyyyMMdd_HHmmss}.csv")
                                .when(header(RecapConstants.FOR).isEqualTo(RecapConstants.STATUS_RECONCILIATION_FAILURE))
                                    .marshal().bindy(BindyType.Csv, StatusReconciliationErrorCSVRecord.class)
                                    .to(RecapCommonConstants.SFTP + ftpUserName + RecapCommonConstants.AT + statusReconciliation + RecapCommonConstants.PRIVATE_KEY_FILE + ftpPrivateKey + RecapCommonConstants.KNOWN_HOST_FILE + ftpKnownHost + "&fileName=StatusReconciliationFailure-${date:now:yyyyMMdd_HHmmss}.csv")
                                    .log("status reconciliation failure report generated in ftp");
                }
            });
        }catch (Exception e){
            logger.error(RecapCommonConstants.LOG_ERROR,e);
        }
    }
}