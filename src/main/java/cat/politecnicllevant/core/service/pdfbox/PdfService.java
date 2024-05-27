package cat.politecnicllevant.core.service.pdfbox;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class PdfService {
    public List<String> getSignatureNames(PDDocument pdf) throws CMSException {
        List<String> names = new ArrayList<>();
        for (PDSignature signature : pdf.getSignatureDictionaries()) {

            COSDictionary sigDict = signature.getCOSObject();
            for (COSName key : sigDict.keySet()) {
                // treure la signatura en si
                if (key.getName().equals("Contents")) {
                    COSString cos = (COSString) sigDict.getDictionaryObject(key);
                    CMSSignedData signedData = new CMSSignedData(cos.getBytes());
                    // treure certificats de la signatura
                    List<SignerInformation> signers = signedData.getSignerInfos().getSigners().stream().toList();

                    Store<X509CertificateHolder> certs = signedData.getCertificates();
                    for (SignerInformation signer : signers) {
                        // treure la informació del signant dins el certificat
                        Collection<X509CertificateHolder> certCollection = certs.getMatches(signer.getSID());
                        for (X509CertificateHolder certHolder : certCollection) {
                            List<String> signerInfo = Arrays.stream(
                                            certHolder.getSubject().toString().split(","))
                                    .filter(s -> s.contains("GIVENNAME") || s.contains("SURNAME"))
                                    .toList();
                            String fullName = signerInfo.get(0)
                                    .substring(signerInfo.get(0).indexOf("=") + 1)
                                    .concat(" "+ signerInfo.get(1).substring(signerInfo.get(1).indexOf("=") + 1));

                            names.add(fullName);
                        }
                    }
                }
            }
        }
        return names;
    }
}