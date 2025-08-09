package com.oddlabs.registration;

public strictfp interface RegServiceInterface {
    public static final String PRIVATE_KEY_FILE = "private_reg_key";
    public static final String PUBLIC_KEY_FILE = "public_reg_key";
    public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGN_ALGORITHM = "SHA1WithRSA";
    public static final int REGSERVICE_PORT = 33215;

    public void register(RegistrationRequest reg_request);
}
