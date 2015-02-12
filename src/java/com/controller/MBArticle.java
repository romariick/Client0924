/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.metier.Article;
import com.parseur.ArticleHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Romaric
 */
@ViewScoped
@ManagedBean(name = "MBArticle")
public class MBArticle implements Serializable {

    /**
     * @return the sauvegardeNonPayez
     */
    public static Double getSauvegardeNonPayez() {
        return sauvegardeNonPayez;
    }

    /**
     * @param aSauvegardeNonPayez the sauvegardeNonPayez to set
     */
    public static void setSauvegardeNonPayez(Double aSauvegardeNonPayez) {
        sauvegardeNonPayez = aSauvegardeNonPayez;
    }

    public List<Article> lstArticle = new ArrayList<Article>();
    private List<Article> lstAchat = new ArrayList<Article>();
    private List<Article> tempAchat = new ArrayList<Article>();
    private static List<Article> sauvegardeAchatNonPayez = new ArrayList<Article>();
    private Double netApayez = 0.0;
    private static Double sauvegardeNonPayez = 0.0;
    private String codebarre = "";
    private Integer idcategorie;
    private boolean recupSelectionner;
    JasperPrint jasperPrint;
    private String fileNameTemp;

    @PostConstruct
    public void init() {
        try {

            parserXML("http://localhost:8080/CaisseApplication-war/webresources/listearticle", "GET");
            lstArticle.clear();
            lstArticle = ArticleHandler.getListArctile();
            if (!sauvegardeAchatNonPayez.isEmpty()) {
                tempAchat = sauvegardeAchatNonPayez;
                netApayez = sauvegardeNonPayez;
            }
        } catch (Exception ex) {
            Logger.getLogger(MBArticle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String listerArticle(String adresseUrl, String methodeHTTP) throws MalformedURLException, IOException {

        URL url = new URL(adresseUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(methodeHTTP);
        StringBuilder sb = new StringBuilder();
        conn.setRequestProperty("Accept", "application/xml");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        String output;

        while ((output = br.readLine()) != null) {
            sb.append(output);
        }
        conn.disconnect();

        return sb.toString();
    }

    public void parserXML(String url, String method) throws Exception {
        String str = listerArticle(url, method);
        stringToDom(str);
    }

    public void parserXMLParCategorie(String url, String method) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        String str = listerArticle(url, method);
        String textXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<article> ";
        String baliseFermante = "</article>";

        String res = textXML.concat(str).concat(baliseFermante);
        stringToDom(res);
    }

    void stringToDom(String xmlSource) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(xmlSource)));
        // Use a Transformer for output
        TransformerFactory tFactory = TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = tFactory.newTransformer();

        DOMSource source = new DOMSource(doc);

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new File("G:\\test.xml"));
        transformer.transform(source, result);

        parserXMLFichier("G:\\test.xml");
    }

    public void parserXMLFichier(String nomFichier) {
        try {
            // création d'une fabrique de parseurs SAX
            SAXParserFactory fabrique = SAXParserFactory.newInstance();

            // création d'un parseur SAX
            SAXParser parseur = fabrique.newSAXParser();

            // lecture d'un fichier XML avec un DefaultHandler
            File fichier = new File(nomFichier);
            DefaultHandler gestionnaire = new ArticleHandler();
            parseur.parse(fichier, gestionnaire);
        } catch (ParserConfigurationException e) {
            System.err.println("Probleme lors de la creation du parser : " + e);
        } catch (SAXException e) {
            System.err.println("Probleme de parsing : " + e);
        } catch (IOException e) {
            System.err.println("Probleme d'entrée/sortie : " + e);
        }

    }

    public void acheterProduit() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        String str = listerArticle("http://localhost:8080/CaisseApplication-war/webresources/listearticle/obtenirArticleByCodeBarre/" + codebarre, "POST");

        if (!str.isEmpty()) {
            stringToDom(str);
            if (!codebarre.isEmpty()) {
                String acheter = listerArticle("http://localhost:8080/CaisseApplication-war/webresources/listearticle/acheterArticleByCodeBarre/" + codebarre, "POST");
                if (acheter.equals("achatok")) {
                    lstAchat = ArticleHandler.getListArctile();

                    Article sauvarticle = new Article();

                    sauvarticle.setCodeBarre(lstAchat.get(0).getCodeBarre());
                    sauvarticle.setPrix(lstAchat.get(0).getPrix());
                    sauvarticle.setNomproduit(lstAchat.get(0).getNomproduit());
                    sauvarticle.setDisponibilite(lstAchat.get(0).getDisponibilite());
                    sauvarticle.setReduction(lstAchat.get(0).getReduction());

                    netApayez += Double.parseDouble(lstAchat.get(0).getPrix());
                    tempAchat.add(sauvarticle);

                    sauvegardeAchatNonPayez = tempAchat;
                    sauvegardeNonPayez = netApayez;

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Achat avec succès !", "");
                    FacesContext.getCurrentInstance().addMessage(null, message);

                }
            }
        }

    }

    public void obtenireParCategorie() {
        try {
            parserXMLParCategorie("http://localhost:8080/CaisseApplication-war/webresources/listearticle/obtenirArticleByCategorie/" + idcategorie.toString(), "POST");

        } catch (Exception ex) {
            Logger.getLogger(MBArticle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void recupererListeProduitSelectionner() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String codebarre = params.get("codebarre");

        String str = listerArticle("http://localhost:8080/CaisseApplication-war/webresources/listearticle/obtenirArticleByCodeBarre/" + codebarre, "POST");

        if (!str.isEmpty()) {
            stringToDom(str);
        }

    }

    public void validerAchat() throws JRException, FileNotFoundException {
        initialisationDetailPDF();
        sauvegardeAchatNonPayez.clear();
        sauvegardeNonPayez = 0.0;
        netApayez = 0.0;

    }

    public void initialisationDetailPDF() throws JRException, FileNotFoundException {
        fileNameTemp = "facture.pdf";

        try {
            String destFiles = FacesContext.getCurrentInstance().getExternalContext().getRealPath("admin/");
            File pdfFile = new File(destFiles + "/Fichier.pdf");
            copyFile(fileNameTemp, new FileInputStream(pdfFile), destFiles);
            String destFileTemp = FacesContext.getCurrentInstance().getExternalContext().getRealPath("admin/" + fileNameTemp);
            File pdfFileTemp = new File(destFileTemp);
            System.out.println("Exist" + pdfFileTemp.exists());
        } catch (Exception e) {
        }

        HashMap mesParametres = new HashMap();

        mesParametres.put("totalEuro", netApayez);
        mesParametres.put("datefacture", new Date());

        List<Article> temps = null;
        JRBeanCollectionDataSource beanCollectionDataSource = new JRBeanCollectionDataSource(tempAchat);
        String reportPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("reports/factureticket.jasper");

        String destFileTemp = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/admin/" + fileNameTemp);
        File pdfFileTemp = new File(destFileTemp);

        jasperPrint = JasperFillManager.fillReport(reportPath, mesParametres, beanCollectionDataSource);

        JRPdfExporter exp = new JRPdfExporter();

        exp.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
        exp.setParameter(JRExporterParameter.OUTPUT_STREAM, new FileOutputStream(pdfFileTemp));

        exp.exportReport();
    }

    public void copyFile(String fileName, InputStream in, String destination) {

        try {

            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(new File(destination + "/" + fileName));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            in.close();
            out.flush();
            out.close();

            System.out.println("New file created!");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * @return the lstArticle
     */
    public List<Article> getLstArticle() {
        return lstArticle;
    }

    /**
     * @param lstArticle the lstArticle to set
     */
    public void setLstArticle(List<Article> lstArticle) {
        this.lstArticle = lstArticle;
    }

    /**
     * @return the codebarre
     */
    public String getCodebarre() {
        return codebarre;
    }

    /**
     * @param codebarre the codebarre to set
     */
    public void setCodebarre(String codebarre) {
        this.codebarre = codebarre;
    }

    /**
     * @return the lstAchat
     */
    public List<Article> getLstAchat() {
        return lstAchat;
    }

    /**
     * @param lstAchat the lstAchat to set
     */
    public void setLstAchat(List<Article> lstAchat) {
        this.lstAchat = lstAchat;
    }

    /**
     * @return the tempAchat
     */
    public List<Article> getTempAchat() {
        return tempAchat;
    }

    /**
     * @param tempAchat the tempAchat to set
     */
    public void setTempAchat(List<Article> tempAchat) {
        this.tempAchat = tempAchat;
    }

    /**
     * @return the netApayez
     */
    public Double getNetApayez() {
        return netApayez;
    }

    /**
     * @param netApayez the netApayez to set
     */
    public void setNetApayez(Double netApayez) {
        this.netApayez = netApayez;
    }

    /**
     * @return the idcategorie
     */
    public Integer getIdcategorie() {
        return idcategorie;
    }

    /**
     * @param idcategorie the idcategorie to set
     */
    public void setIdcategorie(Integer idcategorie) {
        this.idcategorie = idcategorie;
    }

    /**
     * @return the recupSelectionner
     */
    public boolean isRecupSelectionner() {
        return recupSelectionner;
    }

    /**
     * @param recupSelectionner the recupSelectionner to set
     */
    public void setRecupSelectionner(boolean recupSelectionner) {
        this.recupSelectionner = recupSelectionner;
    }

    /**
     * @return the sauvegardeAchatNonPayez
     */
    public static List<Article> getSauvegardeAchatNonPayez() {
        return sauvegardeAchatNonPayez;
    }

    /**
     * @param sauvegardeAchatNonPayez the sauvegardeAchatNonPayez to set
     */
    public static void setSauvegardeAchatNonPayez(List<Article> sauvegardeAchatNonPayez) {
        sauvegardeAchatNonPayez = sauvegardeAchatNonPayez;
    }

    /**
     * @return the fileNameTemp
     */
    public String getFileNameTemp() {
        return fileNameTemp;
    }

    /**
     * @param fileNameTemp the fileNameTemp to set
     */
    public void setFileNameTemp(String fileNameTemp) {
        this.fileNameTemp = fileNameTemp;
    }

}
