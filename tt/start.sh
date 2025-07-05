export nativelib = $(pwd)

cd ../common

java -ea -Djava.library.path=${nativelib} -Xmx1G -Djdk.crypto.KeyAgreement.legacyKDF=true -cp ".:*" com.oddlabs.tt.Main