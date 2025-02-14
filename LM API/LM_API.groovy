// (C) 2023 Aqueduct Technologies LLC
import groovy.json.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.HttpEntity
import org.apache.http.impl.client.*
import org.apache.http.util.EntityUtils

def LM_API(httpVerb, endpointPath, creds="lmaccess", data = [:], queryParams=[:]){
    def api_id = hostProps.get("${creds}.id")
    def api_key = hostProps.get("${creds}.key")
    def api_company = hostProps.get("${creds}.company")
    def queryParamsString = queryParams.collect{k,v->"${k}=${v}"}.join("&")
    def url = "https://${api_company}.logicmonitor.com/santaba/rest${endpointPath}?${queryParamsString}"
    epoch_time = System.currentTimeMillis()
    def json_data = JsonOutput.toJson(data)
    if ((httpVerb == "GET") || (httpVerb == "DELETE")){requestVars = httpVerb + epoch_time + endpointPath}
    else {requestVars = httpVerb + epoch_time + json_data + endpointPath}
    hmac = Mac.getInstance("HmacSHA256")
    secret = new SecretKeySpec(api_key.getBytes(), "HmacSHA256")
    hmac.init(secret)
    hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()))
    signature = hmac_signed.bytes.encodeBase64()
    CloseableHttpClient httpclient = HttpClients.createDefault()
    if (httpVerb == "GET"){http_request = new HttpGet(url)}
    else if (httpVerb == "PATCH"){
        http_request = new HttpPatch(url)
        http_request.setEntity(new StringEntity(json_data, ContentType.APPLICATION_JSON))}
    else if (httpVerb == "PUT"){
        http_request = new HttpPut(url)
        http_request.setEntity(new StringEntity(json_data, ContentType.APPLICATION_JSON))}
    else if (httpVerb == "POST"){
        http_request = new HttpPost(url)
        http_request.setEntity(new StringEntity(json_data, ContentType.APPLICATION_JSON))}
    else {http_request = null}
    if (http_request){
        http_request.setHeader("Authorization" , "LMv1 " + api_id + ":" + signature + ":" + epoch_time)
        http_request.setHeader("Accept", "application/json")
        http_request.setHeader("Content-type", "application/json")
        http_request.setHeader("X-Version", "3")
        response = httpclient.execute(http_request)
        body = EntityUtils.toString(response.getEntity())
        code = response.getStatusLine().getStatusCode()
        return ["body":new JsonSlurper().parseText(body), "code":code]}
    else {return ["body":"", "code": -1]}
}