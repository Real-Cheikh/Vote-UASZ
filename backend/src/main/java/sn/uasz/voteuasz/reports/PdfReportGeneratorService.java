package sn.uasz.voteuasz.reports;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.results.CandidateResultDto;
import sn.uasz.voteuasz.results.ElectionResultDto;
import sn.uasz.voteuasz.results.ResultsService;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PdfReportGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportGeneratorService.class);

    private final ElectionRepository electionRepository;
    private final ResultsService resultsService;
    private final OfficialReportRepository reportRepository;
    private final AuditService auditService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final Color UASZ_GREEN = new Color(16, 94, 56);
    private static final Color UASZ_GOLD = new Color(201, 162, 39);
    private static final Color LIGHT_GRAY = new Color(245, 247, 250);

    public PdfReportGeneratorService(ElectionRepository electionRepository,
                                     ResultsService resultsService,
                                     OfficialReportRepository reportRepository,
                                     AuditService auditService) {
        this.electionRepository = electionRepository;
        this.resultsService = resultsService;
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    @Transactional
    public byte[] generateElectionReportPdf(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        ElectionResultDto results = resultsService.getElectionResultsForCommission(electionId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // En-tête officiel République du Sénégal & UASZ
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, UASZ_GREEN);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, UASZ_GREEN);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            Paragraph headerRepublique = new Paragraph("RÉPUBLIQUE DU SÉNÉGAL\nUn Peuple - Un But - Une Foi", headerFont);
            headerRepublique.setAlignment(Element.ALIGN_CENTER);
            document.add(headerRepublique);

            Paragraph headerUniv = new Paragraph("UNIVERSITÉ ASSANE SECK DE ZIGUINCHOR (UASZ)\nCOMMISSION ÉLECTORALE UNIVERSITAIRE OFFICIELLE\n\n", subHeaderFont);
            headerUniv.setAlignment(Element.ALIGN_CENTER);
            document.add(headerUniv);

            // Titre du Procès-Verbal
            Paragraph pvTitle = new Paragraph("PROCÈS-VERBAL OFFICIEL DE DÉPOUILLEMENT\n", titleFont);
            pvTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pvTitle);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Paragraph datePar = new Paragraph("Date d'édition : " + OffsetDateTime.now().format(dtf) + " (Heure de Dakar)\n\n", smallFont);
            datePar.setAlignment(Element.ALIGN_CENTER);
            document.add(datePar);

            // Caractéristiques du Scrutin
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);

            addInfoRow(infoTable, "Scrutin :", election.getTitre(), boldFont, bodyFont);
            addInfoRow(infoTable, "Type d'élection :", election.getType().name(), boldFont, bodyFont);
            addInfoRow(infoTable, "Collège électoral :", election.getCollege() != null ? election.getCollege().getNom() : "Non spécifié", boldFont, bodyFont);
            addInfoRow(infoTable, "Période du scrutin :", "Du " + election.getDateOuverture().format(dtf) + " au " + election.getDateCloture().format(dtf), boldFont, bodyFont);
            addInfoRow(infoTable, "Statut officiel :", election.getStatut().name(), boldFont, bodyFont);
            document.add(infoTable);

            // Statistiques officielles de participation
            Paragraph statTitle = new Paragraph("1. STATISTIQUES OFFICIELLES DE PARTICIPATION\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, UASZ_GREEN));
            statTitle.setSpacingAfter(8f);
            document.add(statTitle);

            PdfPTable statsTable = new PdfPTable(5);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(15f);

            addTableHeader(statsTable, new String[]{"Inscrits", "Votants", "Bulletins Valides", "Votes Blancs", "Participation"}, boldFont);
            addTableCell(statsTable, String.valueOf(results.getInscritsTotal()), bodyFont);
            addTableCell(statsTable, String.valueOf(results.getVotantsTotal()), bodyFont);
            addTableCell(statsTable, String.valueOf(results.getBulletinsValides()), bodyFont);
            addTableCell(statsTable, String.valueOf(results.getBulletinsBlancs()), bodyFont);
            addTableCell(statsTable, String.format("%.2f %%", results.getTauxParticipation()), boldFont);
            document.add(statsTable);

            // Résultats nominatifs par candidat
            Paragraph resTitle = new Paragraph("2. SUFFRAGES EXPRIMÉS PAR CANDIDAT\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, UASZ_GREEN));
            resTitle.setSpacingAfter(8f);
            document.add(resTitle);

            PdfPTable resultsTable = new PdfPTable(4);
            resultsTable.setWidthPercentage(100);
            resultsTable.setWidths(new float[]{1.5f, 4.5f, 2f, 2f});
            resultsTable.setSpacingAfter(20f);

            addTableHeader(resultsTable, new String[]{"Rang", "Nom & Prénom du Candidat", "Voix Obtenues", "Pourcentage"}, boldFont);

            int rank = 1;
            for (CandidateResultDto cand : results.getResultatsCandidats()) {
                addTableCell(resultsTable, String.valueOf(rank++), bodyFont);
                addTableCell(resultsTable, cand.getCandidateNom() + " " + cand.getCandidatePrenom(), boldFont);
                addTableCell(resultsTable, String.valueOf(cand.getVoixObtenues()), bodyFont);
                addTableCell(resultsTable, String.format("%.2f %%", cand.getPourcentage()), boldFont);
            }
            document.add(resultsTable);

            // Empreinte d'intégrité et Signatures
            Paragraph certTitle = new Paragraph("3. CERTIFICATION DE CONFORMITÉ & SIGNATURES\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, UASZ_GREEN));
            certTitle.setSpacingAfter(8f);
            document.add(certTitle);

            Paragraph certText = new Paragraph(
                    "La Commission Électorale atteste que les chiffres ci-dessus correspondent fidèlement aux bulletins anonymes " +
                    "extraits de l'urne électronique scellée du système VoteUASZ, sans aucune altération.\n\n", bodyFont);
            document.add(certText);

            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setSpacingAfter(20f);

            PdfPCell sign1 = new PdfPCell(new Phrase("Le Président de la Commission Électorale\n\n\n\n[Signature & Cachet Officiel]", bodyFont));
            sign1.setBorder(Rectangle.NO_BORDER);
            sign1.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell sign2 = new PdfPCell(new Phrase("Les Assesseurs & Représentants\n\n\n\n[Signatures]", bodyFont));
            sign2.setBorder(Rectangle.NO_BORDER);
            sign2.setHorizontalAlignment(Element.ALIGN_CENTER);

            signatureTable.addCell(sign1);
            signatureTable.addCell(sign2);
            document.add(signatureTable);

            // Pied de page cryptographique
            document.close();

            byte[] pdfBytes = baos.toByteArray();
            String sha256 = computeSha256(pdfBytes);
            String reportNum = "PV-UASZ-" + election.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Enregistrement sur disque
            Path reportsDir = Paths.get(uploadDir, "reports");
            Files.createDirectories(reportsDir);
            Path filePath = reportsDir.resolve(reportNum + ".pdf");
            Files.write(filePath, pdfBytes);

            // Persistance de l'enregistrement de procès-verbal officiel
            OfficialReport report = reportRepository.findByElectionId(electionId)
                    .orElse(new OfficialReport());
            report.setNumeroRapport(reportNum);
            report.setElection(election);
            report.setTitre("Procès-Verbal Officiel - " + election.getTitre());
            report.setCheminPdf(filePath.toString());
            report.setEmpreinteSha256(sha256);
            report.setDateGeneration(OffsetDateTime.now());
            report.setSignataireNom("Commission Électorale UASZ");
            report.setSignataireRole("Président de Commission");

            reportRepository.save(report);
            auditService.logEvent("OFFICIAL_REPORT_GENERATED", "Génération du PV officiel " + reportNum + " [SHA256: " + sha256 + "]", electionId, "SUCCESS");

            return pdfBytes;
        } catch (DocumentException | IOException e) {
            throw new BusinessException("Erreur lors de la génération du rapport PDF officiel", e);
        }
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3);
        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }
}
