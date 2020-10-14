package org.recap.camel.accessionreconciliation;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.recap.RecapConstants;
import org.recap.RecapCommonConstants;
import org.recap.camel.EmailPayLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Created by akulak on 22/5/17.
 */
@Service
@Scope("prototype")
public class AccessionReconciliationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AccessionReconciliationEmailService.class);

    @Autowired
    private ProducerTemplate producerTemplate;

    @Value("${pul.email.accession.reconciliation.to}")
    private String pulEmailTo;

    @Value("${cul.email.accession.reconciliation.to}")
    private String culEmailTo;

    @Value("${nypl.email.accession.reconciliation.to}")
    private String nyplEmailTo;

    @Value("${pul.email.accession.reconciliation.cc}")
    private String pulEmailCC;

    @Value("${cul.email.accession.reconciliation.cc}")
    private String culEmailCC;

    @Value("${nypl.email.accession.reconciliation.cc}")
    private String nyplEmailCC;

    private String institutionCode;

    /**
     * Instantiates a new Accession reconciliation email service.
     *
     * @param institutionCode the institution code
     */
    public AccessionReconciliationEmailService(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    /**
     * Process input for accession reconciliation email service.
     *
     * @param exchange the exchange
     */
    public void processInput(Exchange exchange) {
        logger.info("accession email started for {}", institutionCode);
        producerTemplate.sendBodyAndHeader(RecapConstants.EMAIL_Q, getEmailPayLoad(exchange), RecapConstants.EMAIL_BODY_FOR,"AccessionReconcilation");
    }

    /**
     * Get email pay load for accession reconciliation email service.
     *
     * @return the email pay load
     */
    public EmailPayLoad getEmailPayLoad(Exchange exchange){
        EmailPayLoad emailPayLoad = new EmailPayLoad();
        String fileNameWithPath = (String)exchange.getIn().getHeader("CamelFileNameProduced");
        emailIdTo(institutionCode,emailPayLoad);
        logger.info("Accession Reconciliation email sent to : {0} and cc : {1} ",emailPayLoad.getTo(),emailPayLoad.getCc());
        emailPayLoad.setMessageDisplay("Barcode Reconciliation has been completed for "+institutionCode.toUpperCase()+". The report is at the FTP location "+fileNameWithPath);
        return emailPayLoad;
    }

    /**
     * Generate Email To id for accession reconciliation email service.
     *
     * @param institution the institution
     * @param emailPayLoad
     * @return the string
     */
    public String emailIdTo(String institution, EmailPayLoad emailPayLoad) {
        if (RecapCommonConstants.NYPL.equalsIgnoreCase(institution)) {
            emailPayLoad.setCc(nyplEmailCC);
            emailPayLoad.setTo(nyplEmailTo);
        } else if (RecapCommonConstants.COLUMBIA.equalsIgnoreCase(institution)) {
            emailPayLoad.setCc(culEmailCC);
            emailPayLoad.setTo(culEmailTo);
        } else if (RecapCommonConstants.PRINCETON.equalsIgnoreCase(institution)) {
            emailPayLoad.setCc(pulEmailCC);
            emailPayLoad.setTo(pulEmailTo);
        }
        return null;
    }

}