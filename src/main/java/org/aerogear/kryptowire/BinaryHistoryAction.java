package org.aerogear.kryptowire;

import hudson.model.*;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;


public class BinaryHistoryAction implements RunAction2 {
    private BinaryInfo info;
    private transient Run run;
    private BinaryStatus status;
    private transient GlobalConfigurationImpl cfg;


    public BinaryHistoryAction(BinaryInfo info) {
        this.info = info;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/aerogear-kryptowire-plugin/menu-logo.png";
    }

    @Override
    public String getDisplayName() {
        return "Kryptowire Scan Results";
    }

    @Override
    public String getUrlName() {
        return "kryptowire";
    }

    public BinaryInfo getInfo() {
        return info;
    }

    public void setInfo(BinaryInfo info) {
        this.info = info;
    }

    public Run getRun() {
        return run;
    }

    public void setRun(Run run) {
        this.run = run;
    }


    public void setStatus(BinaryStatus status) {
        this.status = status;
    }

    public BinaryStatus getStatus() throws IOException, InterruptedException {
        if (this.status != null) {
            return this.status;
        }
        GlobalConfigurationImpl cfg = this.getCfg();
        KryptowireService kws = new KryptowireServiceImpl(cfg.getKwEndpoint(),  cfg.getKwApiKey());
        if (!kws.isCompleted(this.info.getHash())) {
            return BinaryStatus.notReady();
        }
        JSONObject out = kws.getResult(this.info.getUuid());
        if (out.isNull("threat_score")) {
            return BinaryStatus.notReady();
        }
        this.status = BinaryStatus.fromJSONObject(out);

        File targetFile = new File(getRun().getArtifactsDir() + "/kryptowire.pdf");
        if(!targetFile.exists()) {
            kws.downloadReport(this.info.getHash(), "pdf", targetFile);
        }

        targetFile = new File(getRun().getArtifactsDir() + "/kryptowire-niap.pdf");
        if(!targetFile.exists()) {
            kws.downloadReport(this.info.getHash(), "niap_pdf", targetFile);
        }

        this.getRun().save();
        return this.status;
    }

    public String getExternalLink() {
        GlobalConfigurationImpl cfg = this.getCfg();
        return cfg.getKwEndpoint() + "/#/" + info.getPlatform() + "-report/" + info.getUuid();
    }

    private GlobalConfigurationImpl getCfg() {
         if (this.cfg == null) {
             this.cfg = GlobalConfiguration.all().get(GlobalConfigurationImpl.class);
         }
         return this.cfg;
    }

    public String getReportPath()  {
        return getArchivePath("kryptowire.pdf");
    }

    public String getNIAPReportPath() {
        return getArchivePath("kryptowire-niap.pdf");
    }

    private String getArchivePath(String path)  {
        String rootUrl = Jenkins.getActiveInstance().getRootUrl();
        return rootUrl + getRun().getUrl() + "artifact/" + path;
    }
}
