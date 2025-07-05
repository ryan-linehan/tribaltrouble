$nativelib = $pwd

cd "..\common"

java -ea -D"java.library.path"=${nativelib} -Xmx1G -D"jdk.crypto.KeyAgreement.legacyKDF"=true -cp ".;*" com.oddlabs.tt.Main