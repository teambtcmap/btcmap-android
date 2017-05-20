import android.security.NetworkSecurityPolicy;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * @author Igor Bubelov
 */

@Implements(NetworkSecurityPolicy.class)
public class NetworkSecurityPolicyShadow {
    @Implementation
    public static NetworkSecurityPolicy getInstance() {
        try {
            Class<?> shadow = Class.forName("android.security.NetworkSecurityPolicy");
            return (NetworkSecurityPolicy) shadow.newInstance();
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    @Implementation
    public boolean isCleartextTrafficPermitted(String host) {
        return true;
    }
}