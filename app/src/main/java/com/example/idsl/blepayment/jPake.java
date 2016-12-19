package com.example.idsl.blepayment;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.SynchronousQueue;

import javax.crypto.SecretKey;

/**
 * Created by Doni on 12/18/2016.
 */

public class jPake {



    /* J-PAKE can use pre-computed group parameters, same as DSA and Diffie-Hellman.
	 * The parameters below are taken from Sun's JCE implementation of DSA, which
	 * defines the modulus p up to 1024 bits (equivalent to 80-bit security.)
	 * For longer lengths, you can use the group parameters published by NIST.
	 * http://csrc.nist.gov/groups/ST/toolkit/documents/Examples/DSA2_All.pdf
	 */

    BigInteger p = new BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16);
    BigInteger q = new BigInteger("9760508f15230bccb292b982a2eb840bf0581cf5", 16);
    BigInteger g = new BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16);

    // From NIST,
    // 2048-bit p, 224-bit q and 2048-bit g for 112-bit security, use
	  /*
	 * 3072-bit p, 256-bit q and 3072-bit g for 128-bit security, use
	 */
      /*
     * For 192-bit and 256-bit security, you may want to implement J-PAKE using ECC, but
     * that's beyond the scope of this prototype.
     */

    /*
    Secret key of POS
     */
    public  String s2Str = "123456789101112";

    public final String salt = "SALT";
    String logTimer ;
    private static final String TAG = "Testing";
    /* SignerIDs for Alice and Bob. Obviously, they must be different.
     * In practical implementation, it is worth checking that explicitly.
     */


    public  String IDCustomer= "ID_CUSTOMER";
    public  String  IDPOS= "NULL";
    public String finalSKey = "";
    public String finalNonce ="";
    private long ProtocolTime= 0;

    //constructor
    public jPake(Integer type) {
        if(type.equals(1)){
            p = new BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16);
            q = new BigInteger("9760508f15230bccb292b982a2eb840bf0581cf5", 16);
            g = new BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16);

        }else if(type.equals(2)){
            p = new BigInteger("C196BA05AC29E1F9C3C72D56DFFC6154A033F1477AC88EC37F09BE6C5BB95F51C296DD20D1A28A067CCC4D4316A4BD1DCA55ED1066D438C35AEBAABF57E7DAE428782A95ECA1C143DB701FD48533A3C18F0FE23557EA7AE619ECACC7E0B51652A8776D02A425567DED36EABD90CA33A1E8D988F0BBB92D02D1D20290113BB562CE1FC856EEB7CDD92D33EEA6F410859B179E7E789A8F75F645FAE2E136D252BFFAFF89528945C1ABE705A38DBC2D364AADE99BE0D0AAD82E5320121496DC65B3930E38047294FF877831A16D5228418DE8AB275D7D75651CEFED65F78AFC3EA7FE4D79B35F62A0402A1117599ADAC7B269A59F353CF450E6982D3B1702D9CA83", 16);
            q = new BigInteger("90EAF4D1AF0708B1B612FF35E0A2997EB9E9D263C9CE659528945C0D", 16);
            g = new BigInteger("A59A749A11242C58C894E9E5A91804E8FA0AC64B56288F8D47D51B1EDC4D65444FECA0111D78F35FC9FDD4CB1F1B79A3BA9CBEE83A3F811012503C8117F98E5048B089E387AF6949BF8784EBD9EF45876F2E6A5A495BE64B6E770409494B7FEE1DBB1E4B2BC2A53D4F893D418B7159592E4FFFDF6969E91D770DAEBD0B5CB14C00AD68EC7DC1E5745EA55C706C4A1C5C88964E34D09DEB753AD418C1AD0F4FDFD049A955E5D78491C0B7A2F1575A008CCD727AB376DB6E695515B05BD412F5B8C2F4C77EE10DA48ABD53F5DD498927EE7B692BBBCDA2FB23A516C5B4533D73980B2A3B60E384ED200AE21B40D273651AD6060C13D97FD69AA13C5611A51B9085", 16);

        }
        else{
            p = new BigInteger("90066455B5CFC38F9CAA4A48B4281F292C260FEEF01FD61037E56258A7795A1C7AD46076982CE6BB956936C6AB4DCFE05E6784586940CA544B9B2140E1EB523F009D20A7E7880E4E5BFA690F1B9004A27811CD9904AF70420EEFD6EA11EF7DA129F58835FF56B89FAA637BC9AC2EFAAB903402229F491D8D3485261CD068699B6BA58A1DDBBEF6DB51E8FE34E8A78E542D7BA351C21EA8D8F1D29F5D5D15939487E27F4416B0CA632C59EFD1B1EB66511A5A0FBF615B766C5862D0BD8A3FE7A0E0DA0FB2FE1FCB19E8F9996A8EA0FCCDE538175238FC8B0EE6F29AF7F642773EBE8CD5402415A01451A840476B2FCEB0E388D30D4B376C37FE401C2A2C2F941DAD179C540C1C8CE030D460C4D983BE9AB0B20F69144C1AE13F9383EA1C08504FB0BF321503EFE43488310DD8DC77EC5B8349B8BFE97C2C560EA878DE87C11E3D597F1FEA742D73EEC7F37BE43949EF1A0D15C3F3E3FC0A8335617055AC91328EC22B50FC15B941D3D1624CD88BC25F3E941FDDC6200689581BFEC416B4B2CB73", 16);
            q = new BigInteger("CFA0478A54717B08CE64805B76E5B14249A77A4838469DF7F7DC987EFCCFB11D", 16);
            g = new BigInteger("5E5CBA992E0A680D885EB903AEA78E4A45A469103D448EDE3B7ACCC54D521E37F84A4BDD5B06B0970CC2D2BBB715F7B82846F9A0C393914C792E6A923E2117AB805276A975AADB5261D91673EA9AAFFEECBFA6183DFCB5D3B7332AA19275AFA1F8EC0B60FB6F66CC23AE4870791D5982AAD1AA9485FD8F4A60126FEB2CF05DB8A7F0F09B3397F3937F2E90B9E5B9C9B6EFEF642BC48351C46FB171B9BFA9EF17A961CE96C7E7A7CC3D3D03DFAD1078BA21DA425198F07D2481622BCE45969D9C4D6063D72AB7A0F08B2F49A7CC6AF335E08C4720E31476B67299E231F8BD90B39AC3AE3BE0C6B6CACEF8289A2E2873D58E51E029CAFBD55E6841489AB66B5B4B9BA6E2F784660896AFF387D92844CCB8B69475496DE19DA2E58259B090489AC8E62363CDF82CFD8EF2A427ABCD65750B506F56DDE3B988567A88126B914D7828E2B63A6D7ED0747EC59E0E0A23CE7D8A74C1D2C2A7AFB6A29799620F00E11C33787F7DED3B30E1A22D09F1FBDA1ABBBFBF25CAE05A13F812E34563F99410E73B", 16);

        }
    }




    public void setPassword(String password){
        this.s2Str = password;
    }
    public String getfinalSKey (){
        return this.finalSKey;
    }
    public String getFinalNonce(){
        return this.finalNonce;
    }
    public BigInteger x1,x2,s1,gx1,gx2,sigX1_1,sigX1_2,sigX2_1,sigX2_2;
    public byte[] jpakeRound1() throws IOException {

        /**********************
            generate x1 and x1
        **********************/

         x1 = new BigInteger (160, new SecureRandom());
         x2 = new BigInteger (160, new SecureRandom());
        /**********************
         generate gX3 and gX4
         **********************/
         gx1 = g.modPow(x1,p);
         gx2 = g.modPow(x2,p);

        /**********************
         generate ZKP
         **********************/
        BigInteger[] sigX1 = generateZKP(p,q,g,gx1,x1,IDCustomer);
        BigInteger[] sigX2 = generateZKP(p,q,g,gx2,x2,IDCustomer);
        sigX1_1 = sigX1[0];
        sigX1_2 = sigX1[1];
        sigX2_1 = sigX2[0];
        sigX2_2 = sigX2[1];
        List<String> result = new ArrayList<String>();
            result.add("1");
            result.add(IDCustomer);
            result.add(x1.toString());
            result.add(x2.toString());
            result.add(sigX1_1.toString());
            result.add(sigX1_2.toString());
            result.add(sigX2_1.toString());
            result.add(sigX2_2.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (String element : result) {
            out.writeUTF(element);
        }
        byte[] bytes = baos.toByteArray();
        System.out.println(bytes.length);
        return compress.compress(bytes);
    }
    public byte[] jpakeRound2() throws IOException {
        BigInteger s1 = new BigInteger(s2Str.getBytes());
        /* Step 1: Client sends */
        /**********************
         generate x1 and x1
         **********************/

        x1 = new BigInteger (160, new SecureRandom());
        x2 = new BigInteger (160, new SecureRandom());
        /**********************
         generate gX3 and gX4
         **********************/
        gx1 = g.modPow(x1,p);
        gx2 = g.modPow(x2,p);

        /**********************
         generate ZKP
         **********************/
        BigInteger[] sigX1 = generateZKP(p,q,g,gx1,x1,IDCustomer);
        BigInteger[] sigX2 = generateZKP(p,q,g,gx2,x2,IDCustomer);
        sigX1_1 = sigX1[0];
        sigX1_2 = sigX1[1];
        sigX2_1 = sigX2[0];
        sigX2_2 = sigX2[1];
        List<String> result = new ArrayList<String>();
        result.add("1");
        result.add(IDCustomer);
        result.add(x1.toString());
        result.add(x2.toString());
        result.add(sigX1_1.toString());
        result.add(sigX1_2.toString());
        result.add(sigX2_1.toString());
        result.add(sigX2_2.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (String element : result) {
            out.writeUTF(element);
        }
        byte[] bytes = baos.toByteArray();
        System.out.println(bytes.length);
        return compress.compress(bytes);
    }

//    public String run(Integer type){
//        /* Alice verifies Bob's ZKPs and also check g^{x4} != 1*/
//        if (gx4.equals(BigInteger.ONE) || !verifyZKP(p,q,g,gx3,sigX3,IDPOS) ||
//                !verifyZKP(p,q,g,gx4,sigX4,IDPOS)) {
//
//            //System.out.println("g^{x4} shouldn't be 1 or invalid KP{x3,x4}");
//            //System.exit(0);
//        }else {
//            elapsedTime("Verification Alice");
////            System.out.println("Alice checks g^{x4}!=1: OK");
////            System.out.println("Alice checks KP{x3}: OK");
////            System.out.println("Alice checks KP{x4}: OK");
////            System.out.println("");
//        }
//
//        /* Bob verifies Alice's ZKPs */
//        if (gx2.equals(BigInteger.ONE) || !verifyZKP(p,q,g,gx1,sigX1,IDCustomer) ||
//                !verifyZKP(p,q,g,gx2,sigX2,IDCustomer)) {
//
////            System.out.println("g^{x2} shoudn't be 1 or invalid KP{x1,x2}");
////            System.exit(0);
//        }else{
//
////            System.out.println("Bob checks g^{x2}!=1: OK");
////            System.out.println("Bob checks KP{x1},: OK");
////            System.out.println("Bob checks KP{x2},: OK");
////            System.out.println("");
//        }
//
//        //step 2
//
//        BigInteger gA = gx1.multiply(gx3).multiply(gx4).mod(p);
//        BigInteger A = gA.modPow(x2.multiply(s1).mod(q),p);
//        BigInteger[] sigX2s = generateZKP(p,q,gA,A,x2.multiply(s1).mod(q),IDCustomer);
//        elapsedTime("Elapsed Time Step 2");
//        BigInteger gB = gx3.multiply(gx1).multiply(gx2).mod(p);
//        BigInteger B = gB.modPow(x4.multiply(s2).mod(q),p);
//        BigInteger[] sigX4s = generateZKP(p,q,gB,B,x4.multiply(s2).mod(q),IDPOS);
//        startTimer();
//        if (!verifyZKP(p,q,gB,B,sigX4s,IDPOS)){
//
//        } else {
//
//        }
//
//    	/* Bob verifies Alice's ZKP */
//        if (!verifyZKP(p,q,gA,A,sigX2s,IDCustomer)){
//
//        } else {
//
//        }
//
//        BigInteger Ka = getSHA1(gx4.modPow(x2.multiply(s1).negate().mod(q),p).multiply(B).modPow(x2,p));
//        BigInteger Kb = getSHA1(gx2.modPow(x4.multiply(s2).negate().mod(q),p).multiply(A).modPow(x4,p));
//        if(Ka.equals(Kb)) {
//            logTimer += "\n Session Key: (" + Ka.bitLength() + ") Key: " + Ka.toString();
//        }
//        logTimer += "\n Session Key: (" + Ka.bitLength() + ") Key: " + Ka.toString();
//        elapsedTime("Step 2 - Final Session Key Time");
//        logTimer+="\n Total Protocol Time: "+Long.toString(ProtocolTime);
//
//        String keyCalculation = Ka.toString();
//        char[] charArray = keyCalculation.toCharArray();
//
//        byte[] bytes = salt.getBytes();
//
//        final rExt rand = new rExt();
//
//
//        try {
//            SecretKey secretKey = rand.generateKey(charArray,bytes);
//            if (secretKey != null) {finalSKey = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);}
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (InvalidKeySpecException e) {
//            e.printStackTrace();
//        }
//        Log.d("Final Key",finalSKey);
//        logTimer+="\nFinal Key: "+finalSKey;
//        elapsedTime("Elapsed Time round rExt ");
//        keyCalculation = Kb.toString();
//        charArray = keyCalculation.toCharArray();
//        bytes = (IDCustomer+IDPOS).getBytes();
//
//
//        try {
//            SecretKey secretKey = rand.generateKey(charArray,bytes);
//            if (secretKey != null) {finalNonce = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);}
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (InvalidKeySpecException e) {
//            e.printStackTrace();
//        }
//        Log.d("Final Nonce ",finalNonce);
//        logTimer+="\nFinal Nonce : "+finalNonce;
//
//
//        //secret sharing
//
//        //BigInteger shares = SecretSharing();
//        return logTimer;
//
//
//    }

    public BigInteger[] generateZKP (BigInteger p, BigInteger q, BigInteger g,
                                     BigInteger gx, BigInteger x, String signerID){
        BigInteger [] ZKP = new BigInteger [2];

    	/* Generate a random v, and compute g^v */
        BigInteger v = new BigInteger (160, new SecureRandom());
        BigInteger gv = g.modPow(v,p);
        BigInteger h = getSHA1(g,gv,gx,signerID); // h

        ZKP[0] = gv;
        ZKP[1] = v.subtract(x.multiply(h)).mod(q); // r = v-x*h


        return ZKP;
    }

    public boolean verifyZKP(BigInteger p, BigInteger q, BigInteger g, BigInteger gx,
                             BigInteger[] sig, String signerID) {

    	/* sig={g^v,r} */
        BigInteger h = getSHA1(g,sig[0],gx,signerID);
        if (gx.compareTo(BigInteger.ZERO) == 1 && // g^x > 0
                gx.compareTo(p.subtract(BigInteger.ONE)) == -1 && // g^x < p-1
                gx.modPow(q, p).compareTo(BigInteger.ONE) == 0 && // g^x^q = 1
    			/* Below, I took an straightforward way to compute g^r * g^x^h, which needs 2 exp. Using
    			 * a simultaneous computation technique would only need 1 exp.
    			 */
                g.modPow(sig[1],p).multiply( gx.modPow(h,p) ).mod(p).compareTo(sig[0]) == 0) // g^v=g^r * g^x^h
            return true;
        else
            return false;
    }

    public BigInteger getSHA1(BigInteger g, BigInteger gr, BigInteger gx, String signerID) {

        MessageDigest sha = null;

        try {
            sha = MessageDigest.getInstance("SHA-1");

    		/* Note: you should ensure the items in H(...) have clear boundaries.
    		 * It is simple if the other party knows sizes of g, gr, gx
    		 * and signerID and hence the boundary is unambiguous. If not, you'd
    		 * better prepend each item with its byte length, but I've
    		 * omitted that here.
    		 */

            sha.update(g.toByteArray());
            sha.update(gr.toByteArray());
            sha.update(gx.toByteArray());
            sha.update(signerID.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new BigInteger(sha.digest());
    }

    public BigInteger getSHA1(BigInteger K) {

        MessageDigest sha = null;

        try {
            sha = MessageDigest.getInstance("SHA-1");
            sha.update(K.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new BigInteger(1, sha.digest()); // 1 for positive int
    }

    public void SecretSharing(){
        final Sss shamir = new Sss(3, 4);

        final BigInteger secret = new BigInteger("123456789101112");
        final Sss.SecretShare[] shares = shamir.split(secret);
        final BigInteger prime = shamir.getPrime();
        for(Sss.SecretShare share: shares){
            Log.d("Secret Share:",share.toString());
        }
        Log.d("Prime :",prime.toString());
        //final Sss shamir2 = new Sss(3, 4);
        //final BigInteger result = shamir2.combine(shares, prime);
    }
}
