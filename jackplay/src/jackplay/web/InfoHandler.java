package jackplay.web;

import com.sun.net.httpserver.HttpExchange;
import jackplay.play.InfoCenter;
import jackplay.bootstrap.TraceKeeper;

import java.util.Map;

public class InfoHandler extends BaseHandler {
    InfoCenter infoCenter;

    public InfoHandler(InfoCenter infoCenter) {
        this.infoCenter = infoCenter;
    }

    @Override
    public void process(HttpExchange http, String uri, Map<String, String> params) throws Exception {
        switch (getUriPath(uri)) {
            case "/info/traceLogs":
                CommonHandling.willReturnJson(http);
                CommonHandling.serveStringBody(http, 200, JSON.objectToJson(TraceKeeper.getTraceLogs()));
                break;
            case "/info/loadedMethods":
                CommonHandling.willReturnJson(http);
                CommonHandling.serveStringBody(http, 200, JSON.objectToJson(infoCenter.getLoadedMethods()));
                break;
            case "/info/clearTraceLogs":
                TraceKeeper.clearLogHistory();
                CommonHandling.serveStringBody(http, 200, "OK");
                break;
            case "/info/settings":
                CommonHandling.willReturnJson(http);
                CommonHandling.serveStringBody(http, 200, JSON.objectToJson(infoCenter.getServerSettings()));
                break;
            case "/info/updateSettings":
                CommonHandling.willReturnJson(http);
                infoCenter.updateOption("traceLogLimit", params.get("traceLogLimit"));
                infoCenter.updateOption("autoSuggestLimit", params.get("autoSuggestLimit"));
                CommonHandling.serveStringBody(http, 200, JSON.objectToJson(infoCenter.getServerSettings()));
                break;
            case "/info/currentProgram":
                CommonHandling.willReturnJson(http);
                CommonHandling.serveStringBody(http, 200, JSON.objectToJson(infoCenter.getCurrentProgram()));
                break;
            default:
                CommonHandling.error_404(http);
        }
    }
}