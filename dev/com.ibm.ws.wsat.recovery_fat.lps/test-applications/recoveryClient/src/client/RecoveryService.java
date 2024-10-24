//
// Generated By:JAX-WS RI IBM 2.2.1-07/09/2014 01:52 PM(foreman)- (JAXB RI IBM 2.2.3-07/07/2014 12:54 PM(foreman)-)
//


package client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "RecoveryService", targetNamespace = "http://server/", wsdlLocation = "http://localhost:8010/recoveryServer/RecoveryService?wsdl")
public class RecoveryService
    extends Service
{

    private final static URL RECOVERYSERVICE_WSDL_LOCATION;
    private final static WebServiceException RECOVERYSERVICE_EXCEPTION;
    private final static QName RECOVERYSERVICE_QNAME = new QName("http://server/", "RecoveryService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/recoveryServer/RecoveryService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        RECOVERYSERVICE_WSDL_LOCATION = url;
        RECOVERYSERVICE_EXCEPTION = e;
    }

    public RecoveryService() {
        super(__getWsdlLocation(), RECOVERYSERVICE_QNAME);
    }

    public RecoveryService(URL wsdlLocation) {
        super(wsdlLocation, RECOVERYSERVICE_QNAME);
    }

    public RecoveryService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns Recovery
     */
    @WebEndpoint(name = "RecoveryPort")
    public Recovery getRecoveryPort() {
        return super.getPort(new QName("http://server/", "RecoveryPort"), Recovery.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Recovery
     */
    @WebEndpoint(name = "RecoveryPort")
    public Recovery getRecoveryPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server/", "RecoveryPort"), Recovery.class, features);
    }

    private static URL __getWsdlLocation() {
        if (RECOVERYSERVICE_EXCEPTION!= null) {
            throw RECOVERYSERVICE_EXCEPTION;
        }
        return RECOVERYSERVICE_WSDL_LOCATION;
    }

}
