package com.globalsight.selenium.restful;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class LoginTester extends RestfulApiTestHelper
{
    /**
     * http://localhost:8080/globalsight/restfulServices/companies/York/login-helper?userName=york&password=password
     */
    public void testGetAuthorization()
    {
        CloseableHttpClient httpClient = getHttpClient();
        HttpResponse httpResponse = null;
        try
        {
            String url = "http://10.10.213.20:8080/globalsight/restfulServices/companies/85/login-helper?userName=85all&password=password";
            HttpGet httpGet = new HttpGet(url);

            httpResponse = httpClient.execute(httpGet);

            printHttpResponse(httpResponse);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            consumeQuietly(httpResponse);
        }
    }

    public static void main(String[] args)
    {
        LoginTester tester = new LoginTester();

        try
        {
            tester.testGetAuthorization();
        }
        finally
        {
            tester.shutdownHttpClient();
        }
    }
}
