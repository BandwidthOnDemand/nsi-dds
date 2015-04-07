package net.es.nsi.dds.authorization;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.bouncycastle.asn1.x500.style.AbstractX500NameStyle;

public class ExtendedRFC4519Style extends AbstractX500NameStyle
{
    public static final ASN1ObjectIdentifier businessCategory = new ASN1ObjectIdentifier("2.5.4.15");
    public static final ASN1ObjectIdentifier c = new ASN1ObjectIdentifier("2.5.4.6");
    public static final ASN1ObjectIdentifier cn = new ASN1ObjectIdentifier("2.5.4.3");
    public static final ASN1ObjectIdentifier dc = new ASN1ObjectIdentifier("0.9.2342.19200300.100.1.25");
    public static final ASN1ObjectIdentifier description = new ASN1ObjectIdentifier("2.5.4.13");
    public static final ASN1ObjectIdentifier destinationIndicator = new ASN1ObjectIdentifier("2.5.4.27");
    public static final ASN1ObjectIdentifier distinguishedName = new ASN1ObjectIdentifier("2.5.4.49");
    public static final ASN1ObjectIdentifier dnQualifier = new ASN1ObjectIdentifier("2.5.4.46");
    public static final ASN1ObjectIdentifier enhancedSearchGuide = new ASN1ObjectIdentifier("2.5.4.47");
    public static final ASN1ObjectIdentifier facsimileTelephoneNumber = new ASN1ObjectIdentifier("2.5.4.23");
    public static final ASN1ObjectIdentifier generationQualifier = new ASN1ObjectIdentifier("2.5.4.44");
    public static final ASN1ObjectIdentifier givenName = new ASN1ObjectIdentifier("2.5.4.42");
    public static final ASN1ObjectIdentifier houseIdentifier = new ASN1ObjectIdentifier("2.5.4.51");
    public static final ASN1ObjectIdentifier initials = new ASN1ObjectIdentifier("2.5.4.43");
    public static final ASN1ObjectIdentifier internationalISDNNumber = new ASN1ObjectIdentifier("2.5.4.25");
    public static final ASN1ObjectIdentifier l = new ASN1ObjectIdentifier("2.5.4.7");
    public static final ASN1ObjectIdentifier member = new ASN1ObjectIdentifier("2.5.4.31");
    public static final ASN1ObjectIdentifier name = new ASN1ObjectIdentifier("2.5.4.41");
    public static final ASN1ObjectIdentifier o = new ASN1ObjectIdentifier("2.5.4.10");
    public static final ASN1ObjectIdentifier ou = new ASN1ObjectIdentifier("2.5.4.11");
    public static final ASN1ObjectIdentifier owner = new ASN1ObjectIdentifier("2.5.4.32");
    public static final ASN1ObjectIdentifier physicalDeliveryOfficeName = new ASN1ObjectIdentifier("2.5.4.19");
    public static final ASN1ObjectIdentifier postalAddress = new ASN1ObjectIdentifier("2.5.4.16");
    public static final ASN1ObjectIdentifier postalCode = new ASN1ObjectIdentifier("2.5.4.17");
    public static final ASN1ObjectIdentifier postOfficeBox = new ASN1ObjectIdentifier("2.5.4.18");
    public static final ASN1ObjectIdentifier preferredDeliveryMethod = new ASN1ObjectIdentifier("2.5.4.28");
    public static final ASN1ObjectIdentifier registeredAddress = new ASN1ObjectIdentifier("2.5.4.26");
    public static final ASN1ObjectIdentifier roleOccupant = new ASN1ObjectIdentifier("2.5.4.33");
    public static final ASN1ObjectIdentifier searchGuide = new ASN1ObjectIdentifier("2.5.4.14");
    public static final ASN1ObjectIdentifier seeAlso = new ASN1ObjectIdentifier("2.5.4.34");
    public static final ASN1ObjectIdentifier serialNumber = new ASN1ObjectIdentifier("2.5.4.5");
    public static final ASN1ObjectIdentifier sn = new ASN1ObjectIdentifier("2.5.4.4");
    public static final ASN1ObjectIdentifier st = new ASN1ObjectIdentifier("2.5.4.8");
    public static final ASN1ObjectIdentifier street = new ASN1ObjectIdentifier("2.5.4.9");
    public static final ASN1ObjectIdentifier telephoneNumber = new ASN1ObjectIdentifier("2.5.4.20");
    public static final ASN1ObjectIdentifier teletexTerminalIdentifier = new ASN1ObjectIdentifier("2.5.4.22");
    public static final ASN1ObjectIdentifier telexNumber = new ASN1ObjectIdentifier("2.5.4.21");
    public static final ASN1ObjectIdentifier title = new ASN1ObjectIdentifier("2.5.4.12");
    public static final ASN1ObjectIdentifier uid = new ASN1ObjectIdentifier("0.9.2342.19200300.100.1.1");
    public static final ASN1ObjectIdentifier uniqueMember = new ASN1ObjectIdentifier("2.5.4.50");
    public static final ASN1ObjectIdentifier userPassword = new ASN1ObjectIdentifier("2.5.4.35");
    public static final ASN1ObjectIdentifier x121Address = new ASN1ObjectIdentifier("2.5.4.24");
    public static final ASN1ObjectIdentifier x500UniqueIdentifier = new ASN1ObjectIdentifier("2.5.4.45");

    // RFC 2985 attributes.
    public static final ASN1ObjectIdentifier pKCS7PDU = new ASN1ObjectIdentifier("1.2.840.113549.1.9.25.5");
    public static final ASN1ObjectIdentifier userPKCS12 = new ASN1ObjectIdentifier("2.16.840.1.113730.3.1.216");
    public static final ASN1ObjectIdentifier pKCS15Token = new ASN1ObjectIdentifier("1.2.840.113549.1.9.25.1");
    public static final ASN1ObjectIdentifier encryptedPrivateKeyInfo = new ASN1ObjectIdentifier("1.2.840.113549.1.9.25.2");
    public static final ASN1ObjectIdentifier emailAddress = new ASN1ObjectIdentifier("1.2.840.113549.1.9.1");
    public static final ASN1ObjectIdentifier unstructuredName = new ASN1ObjectIdentifier("1.2.840.113549.1.9.2");
    public static final ASN1ObjectIdentifier unstructuredAddress = new ASN1ObjectIdentifier("1.2.840.113549.1.9.8");
    public static final ASN1ObjectIdentifier dateOfBirth = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.9.1");
    public static final ASN1ObjectIdentifier placeOfBirth = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.9.2");
    public static final ASN1ObjectIdentifier gender = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.9.3");
    public static final ASN1ObjectIdentifier countryOfCitizenship = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.9.4");
    public static final ASN1ObjectIdentifier countryOfResidence = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.9.5");
    public static final ASN1ObjectIdentifier pseudonym = new ASN1ObjectIdentifier("2.5.4.65");
    public static final ASN1ObjectIdentifier contentType = new ASN1ObjectIdentifier("1.2.840.113549.1.9.3");
    public static final ASN1ObjectIdentifier messageDigest = new ASN1ObjectIdentifier("1.2.840.113549.1.9.4");
    public static final ASN1ObjectIdentifier signingTime = new ASN1ObjectIdentifier("1.2.840.113549.1.9.5");
    public static final ASN1ObjectIdentifier counterSignature = new ASN1ObjectIdentifier("1.2.840.113549.1.9.6");
    public static final ASN1ObjectIdentifier challengePassword = new ASN1ObjectIdentifier("1.2.840.113549.1.9.7");

    /**
     * default look up table translating OID values into their common symbols following
     * the convention in RFC 2253 with a few extras
     */
    private static final ConcurrentHashMap<ASN1ObjectIdentifier, String> DefaultSymbols = new ConcurrentHashMap<>();

    /**
     * look up table translating common symbols into their OIDS.
     */
    private static final ConcurrentHashMap<String, ASN1ObjectIdentifier> DefaultLookUp = new ConcurrentHashMap<>();

    static
    {
        DefaultSymbols.put(businessCategory, "businessCategory");
        DefaultSymbols.put(c, "c");
        DefaultSymbols.put(cn, "cn");
        DefaultSymbols.put(dc, "dc");
        DefaultSymbols.put(description, "description");
        DefaultSymbols.put(destinationIndicator, "destinationIndicator");
        DefaultSymbols.put(distinguishedName, "distinguishedName");
        DefaultSymbols.put(dnQualifier, "dnQualifier");
        DefaultSymbols.put(enhancedSearchGuide, "enhancedSearchGuide");
        DefaultSymbols.put(facsimileTelephoneNumber, "facsimileTelephoneNumber");
        DefaultSymbols.put(generationQualifier, "generationQualifier");
        DefaultSymbols.put(givenName, "givenName");
        DefaultSymbols.put(houseIdentifier, "houseIdentifier");
        DefaultSymbols.put(initials, "initials");
        DefaultSymbols.put(internationalISDNNumber, "internationalISDNNumber");
        DefaultSymbols.put(l, "l");
        DefaultSymbols.put(member, "member");
        DefaultSymbols.put(name, "name");
        DefaultSymbols.put(o, "o");
        DefaultSymbols.put(ou, "ou");
        DefaultSymbols.put(owner, "owner");
        DefaultSymbols.put(physicalDeliveryOfficeName, "physicalDeliveryOfficeName");
        DefaultSymbols.put(postalAddress, "postalAddress");
        DefaultSymbols.put(postalCode, "postalCode");
        DefaultSymbols.put(postOfficeBox, "postOfficeBox");
        DefaultSymbols.put(preferredDeliveryMethod, "preferredDeliveryMethod");
        DefaultSymbols.put(registeredAddress, "registeredAddress");
        DefaultSymbols.put(roleOccupant, "roleOccupant");
        DefaultSymbols.put(searchGuide, "searchGuide");
        DefaultSymbols.put(seeAlso, "seeAlso");
        DefaultSymbols.put(serialNumber, "serialNumber");
        DefaultSymbols.put(sn, "sn");
        DefaultSymbols.put(st, "st");
        DefaultSymbols.put(street, "street");
        DefaultSymbols.put(telephoneNumber, "telephoneNumber");
        DefaultSymbols.put(teletexTerminalIdentifier, "teletexTerminalIdentifier");
        DefaultSymbols.put(telexNumber, "telexNumber");
        DefaultSymbols.put(title, "title");
        DefaultSymbols.put(uid, "uid");
        DefaultSymbols.put(uniqueMember, "uniqueMember");
        DefaultSymbols.put(userPassword, "userPassword");
        DefaultSymbols.put(x121Address, "x121Address");
        DefaultSymbols.put(x500UniqueIdentifier, "x500UniqueIdentifier");

        // RFC 2985 attributes.
        DefaultSymbols.put(pKCS7PDU, "pKCS7PDU");
        DefaultSymbols.put(userPKCS12, "userPKCS12");
        DefaultSymbols.put(pKCS15Token, "pKCS15Token");
        DefaultSymbols.put(encryptedPrivateKeyInfo, "encryptedPrivateKeyInfo");
        DefaultSymbols.put(emailAddress, "emailAddress");
        DefaultSymbols.put(unstructuredName, "unstructuredName");
        DefaultSymbols.put(unstructuredAddress, "unstructuredAddress");
        DefaultSymbols.put(dateOfBirth, "dateOfBirth");
        DefaultSymbols.put(placeOfBirth, "placeOfBirth");
        DefaultSymbols.put(gender, "gender");
        DefaultSymbols.put(countryOfCitizenship, "countryOfCitizenship");
        DefaultSymbols.put(pseudonym, "pseudonym");
        DefaultSymbols.put(contentType, "contentType");
        DefaultSymbols.put(messageDigest, "messageDigest");
        DefaultSymbols.put(signingTime, "signingTime");
        DefaultSymbols.put(counterSignature, "counterSignature");
        DefaultSymbols.put(challengePassword, "challengePassword");

        DefaultLookUp.put("businesscategory", businessCategory);
        DefaultLookUp.put("c", c);
        DefaultLookUp.put("cn", cn);
        DefaultLookUp.put("dc", dc);
        DefaultLookUp.put("description", description);
        DefaultLookUp.put("destinationindicator", destinationIndicator);
        DefaultLookUp.put("distinguishedname", distinguishedName);
        DefaultLookUp.put("dnqualifier", dnQualifier);
        DefaultLookUp.put("enhancedsearchguide", enhancedSearchGuide);
        DefaultLookUp.put("facsimiletelephonenumber", facsimileTelephoneNumber);
        DefaultLookUp.put("generationqualifier", generationQualifier);
        DefaultLookUp.put("givenname", givenName);
        DefaultLookUp.put("houseidentifier", houseIdentifier);
        DefaultLookUp.put("initials", initials);
        DefaultLookUp.put("internationalisdnnumber", internationalISDNNumber);
        DefaultLookUp.put("l", l);
        DefaultLookUp.put("member", member);
        DefaultLookUp.put("name", name);
        DefaultLookUp.put("o", o);
        DefaultLookUp.put("ou", ou);
        DefaultLookUp.put("owner", owner);
        DefaultLookUp.put("physicaldeliveryofficename", physicalDeliveryOfficeName);
        DefaultLookUp.put("postaladdress", postalAddress);
        DefaultLookUp.put("postalcode", postalCode);
        DefaultLookUp.put("postofficebox", postOfficeBox);
        DefaultLookUp.put("preferreddeliverymethod", preferredDeliveryMethod);
        DefaultLookUp.put("registeredaddress", registeredAddress);
        DefaultLookUp.put("roleoccupant", roleOccupant);
        DefaultLookUp.put("searchguide", searchGuide);
        DefaultLookUp.put("seealso", seeAlso);
        DefaultLookUp.put("serialnumber", serialNumber);
        DefaultLookUp.put("sn", sn);
        DefaultLookUp.put("st", st);
        DefaultLookUp.put("street", street);
        DefaultLookUp.put("telephonenumber", telephoneNumber);
        DefaultLookUp.put("teletexterminalidentifier", teletexTerminalIdentifier);
        DefaultLookUp.put("telexnumber", telexNumber);
        DefaultLookUp.put("title", title);
        DefaultLookUp.put("uid", uid);
        DefaultLookUp.put("uniquemember", uniqueMember);
        DefaultLookUp.put("userpassword", userPassword);
        DefaultLookUp.put("x121address", x121Address);
        DefaultLookUp.put("x500uniqueidentifier", x500UniqueIdentifier);

        // RFC 2985 attributes.
        DefaultLookUp.put("pkcs7pdu", pKCS7PDU);
        DefaultLookUp.put("userpkcs12", userPKCS12);
        DefaultLookUp.put("pkcs15token", pKCS15Token);
        DefaultLookUp.put("encryptedprivatekeyinfo", encryptedPrivateKeyInfo);
        DefaultLookUp.put("emailaddress", emailAddress);
        DefaultLookUp.put("unstructuredname", unstructuredName);
        DefaultLookUp.put("unstructuredaddress", unstructuredAddress);
        DefaultLookUp.put("dateofbirth", dateOfBirth);
        DefaultLookUp.put("placeofbirth", placeOfBirth);
        DefaultLookUp.put("gender", gender);
        DefaultLookUp.put("countryofcitizenship", countryOfCitizenship);
        DefaultLookUp.put("countryofresidence", countryOfResidence);
        DefaultLookUp.put("pseudonym", pseudonym);
        DefaultLookUp.put("contenttype", contentType);
        DefaultLookUp.put("messagedigest", messageDigest);
        DefaultLookUp.put("signingtime", signingTime);
        DefaultLookUp.put("countersignature", counterSignature);
        DefaultLookUp.put("challengepassword", challengePassword);

        // TODO: need to add correct matching for equality comparisons.
    }

    /**
     * Singleton instance.
     */
    public static final X500NameStyle INSTANCE = new ExtendedRFC4519Style();

    protected final Map<String, ASN1ObjectIdentifier> defaultLookUp;
    protected final Map<ASN1ObjectIdentifier, String> defaultSymbols;

    protected ExtendedRFC4519Style()
    {
        defaultSymbols = Collections.unmodifiableMap(DefaultSymbols);
        defaultLookUp = Collections.unmodifiableMap(DefaultLookUp);
    }

    @Override
    protected ASN1Encodable encodeStringValue(ASN1ObjectIdentifier oid,
    		String value) {
    	if (oid.equals(dc) || oid.equals(emailAddress))
        {
            return new DERIA5String(value);
        }
        else if (oid.equals(c) || oid.equals(serialNumber) || oid.equals(dnQualifier)
            || oid.equals(telephoneNumber) || oid.equals(gender)
            || oid.equals(countryOfCitizenship) || oid.equals(countryOfResidence))
        {
            return new DERPrintableString(value);
        }

    	return super.encodeStringValue(oid, value);
    }

    @Override
    public String oidToDisplayName(ASN1ObjectIdentifier oid)
    {
        return DefaultSymbols.get(oid);
    }

    @Override
    public String[] oidToAttrNames(ASN1ObjectIdentifier oid)
    {
        return ExtendedIETFUtils.findAttrNamesForOID(oid, defaultLookUp);
    }

    @Override
    public ASN1ObjectIdentifier attrNameToOID(String attrName)
    {
        return ExtendedIETFUtils.decodeAttrName(attrName, defaultLookUp);
    }

    // parse backwards
    @Override
    public RDN[] fromString(String dirName)
    {
        RDN[] tmp = ExtendedIETFUtils.rDNsFromString(dirName, this);
        RDN[] res = new RDN[tmp.length];

        for (int i = 0; i != tmp.length; i++)
        {
            res[res.length - i - 1] = tmp[i];
        }

        return res;
    }

    // convert in reverse
    @Override
    public String toString(X500Name name)
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;

        RDN[] rdns = name.getRDNs();

        for (int i = rdns.length - 1; i >= 0; i--)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                buf.append(',');
            }

            ExtendedIETFUtils.appendRDN(buf, rdns[i], defaultSymbols);
        }

        return buf.toString();
    }


}
