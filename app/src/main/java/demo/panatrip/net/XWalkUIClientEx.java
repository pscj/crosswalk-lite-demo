package demo.panatrip.net;

import android.webkit.ValueCallback;

import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

/**
 * Created by pscj on 2016-01-29.
 */
public class XWalkUIClientEx extends XWalkUIClient {

    public XWalkUIClientEx(XWalkView view) {
        super(view);
    }

    @Override
    public void openFileChooser(XWalkView view, final ValueCallback uploadFile, String acceptType, String capture) {
        uploadFile.onReceiveValue(null);
    }
}
