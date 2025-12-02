package com.example.angella.eventsplatform.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PdfReportService {

    private PdfFont russianFont;

    public PdfReportService() {
        initFont();
    }

    private void initFont() {
        try {
            // Сначала пробуем системные шрифты с поддержкой кириллицы
            String[] systemFonts = {
                    StandardFonts.HELVETICA,
                    StandardFonts.TIMES_ROMAN,
                    StandardFonts.COURIER
            };

            for (String fontName : systemFonts) {
                try {
                    russianFont = PdfFontFactory.createFont(fontName, PdfEncodings.IDENTITY_H);
                    log.info("Using system font: {}", fontName);

                    // Проверяем, поддерживает ли шрифт русские символы
                    if (testRussianFont(russianFont)) {
                        break;
                    } else {
                        russianFont = null;
                    }
                } catch (Exception e) {
                    log.debug("System font {} not available: {}", fontName, e.getMessage());
                }
            }

            // Если системные шрифты не подошли, пробуем загрузить из файла
            if (russianFont == null) {
                String[] fontPaths = {
                        "fonts/arial.ttf",
                        "fonts/times.ttf",
                        "fonts/DejaVuSans.ttf"
                };

                for (String fontPath : fontPaths) {
                    try {
                        ClassPathResource fontResource = new ClassPathResource(fontPath);
                        if (fontResource.exists()) {
                            // Читаем файл шрифта в массив байтов
                            byte[] fontData = fontResource.getInputStream().readAllBytes();
                            russianFont = PdfFontFactory.createFont(fontData, PdfEncodings.IDENTITY_H);

                            if (testRussianFont(russianFont)) {
                                log.info("Successfully loaded font from: {}", fontPath);
                                break;
                            } else {
                                russianFont = null;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Failed to load font {}: {}", fontPath, e.getMessage());
                    }
                }
            }

            // Последняя попытка - стандартный шрифт
            if (russianFont == null) {
                russianFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                log.warn("Using default font (Russian characters may not display correctly)");
            }

        } catch (Exception e) {
            log.error("Critical error initializing font: {}", e.getMessage());
            try {
                russianFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            } catch (Exception ex) {
                log.error("Failed to create any font", ex);
            }
        }
    }

    private boolean testRussianFont(PdfFont font) {
        try {
            // Проверяем, отображаются ли русские символы
            String testText = "Тест";
            font.getWidth(testText, 12);
            return true;
        } catch (Exception e) {
            log.debug("Font doesn't support Russian characters: {}", e.getMessage());
            return false;
        }
    }

    public byte[] generateStatisticsReport(Map<String, Object> stats) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Установка шрифта для всего документа
            if (russianFont != null) {
                document.setFont(russianFont);
            }

            document.setMargins(20, 20, 20, 20);
            addTitlePage(document, stats);
            addPlatformOverview(document, stats);
            addUserActivity(document, stats);
            addEventStatistics(document, stats);
            addCompletionRates(document, stats);
            addPopularCategories(document, stats);
            addFooter(document);

            document.close();
            log.info("PDF report generated successfully, size: {} bytes", outputStream.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            return createErrorPdf(e.getMessage());
        }
    }

    private void addTitlePage(Document document, Map<String, Object> stats) {
        Paragraph title = createParagraph("СТАТИСТИЧЕСКИЙ ОТЧЕТ", 20, true, TextAlignment.CENTER);
        title.setMarginBottom(20);
        document.add(title);

        Paragraph subtitle = createParagraph("Events Platform Analytics", 14, false, TextAlignment.CENTER);
        subtitle.setItalic();
        subtitle.setMarginBottom(30);
        document.add(subtitle);

        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        Paragraph date = createParagraph("Дата генерации: " + generatedAt, 10, false, TextAlignment.CENTER);
        date.setMarginBottom(40);
        document.add(date);

        LineSeparator line = new LineSeparator(new SolidLine());
        line.setMarginBottom(30);
        document.add(line);
    }

    private void addPlatformOverview(Document document, Map<String, Object> stats) {
        Paragraph header = createParagraph("ОБЗОР ПЛАТФОРМЫ", 16, true, TextAlignment.LEFT);
        header.setMarginBottom(15);
        document.add(header);

        Map<String, Object> overview = (Map<String, Object>) stats.get("platformOverview");

        Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Заголовки таблицы
        table.addHeaderCell(createHeaderCell("Показатель"));
        table.addHeaderCell(createHeaderCell("Значение"));

        if (overview != null) {
            addTableRow(table, "Общее количество пользователей", formatNumber(overview.get("totalUsers")) + " пользователей");
            addTableRow(table, "Всего мероприятий", formatNumber(overview.get("totalEvents")) + " мероприятий");
            addTableRow(table, "Активных мероприятий", formatNumber(overview.get("activeEvents")) + " предстоящих");
            addTableRow(table, "Категорий мероприятий", formatNumber(overview.get("totalCategories")) + " категорий");
            addTableRow(table, "Среднее количество участников", formatNumber(overview.get("averageParticipants")) + " человек");
        }

        document.add(table);
    }

    private void addUserActivity(Document document, Map<String, Object> stats) {
        Paragraph header = createParagraph("АКТИВНОСТЬ ПОЛЬЗОВАТЕЛЕЙ", 16, true, TextAlignment.LEFT);
        header.setMarginBottom(15);
        document.add(header);

        Map<String, Object> activity = (Map<String, Object>) stats.get("userActivity");

        Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        table.addHeaderCell(createHeaderCell("Вид активности"));
        table.addHeaderCell(createHeaderCell("Количество"));

        if (activity != null) {
            addTableRow(table, "Комментарии к мероприятиям", formatNumber(activity.get("totalComments")) + " комментариев");
            addTableRow(table, "Сообщения в чатах", formatNumber(activity.get("totalChatMessages")) + " сообщений");
            addTableRow(table, "Созданные задачи", formatNumber(activity.get("totalTasksCreated")) + " задач");
            addTableRow(table, "Элементы чек-листов", formatNumber(activity.get("totalChecklistItems")) + " элементов");

            Object messagesPerUser = activity.get("messagesPerUser");
            if (messagesPerUser != null) {
                addTableRow(table, "Сообщений на пользователя", String.format("%.1f сообщений/пользователь", messagesPerUser));
            }
        }

        document.add(table);
    }

    private void addEventStatistics(Document document, Map<String, Object> stats) {
        Paragraph header = createParagraph("СТАТИСТИКА МЕРОПРИЯТИЙ", 16, true, TextAlignment.LEFT);
        header.setMarginBottom(15);
        document.add(header);

        Map<String, Object> events = (Map<String, Object>) stats.get("eventStatistics");

        Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        table.addHeaderCell(createHeaderCell("Показатель"));
        table.addHeaderCell(createHeaderCell("Значение"));

        if (events != null) {
            addTableRow(table, "Всего мероприятий на платформе", formatNumber(events.get("totalEvents")) + " мероприятий");
            addTableRow(table, "Предстоящих мероприятий", formatNumber(events.get("upcomingEvents")) + " активных");
            addTableRow(table, "Среднее количество участников", formatNumber(events.get("averageParticipants")) + " человек");

            Object eventsWithChats = events.get("eventsWithChats");
            if (eventsWithChats != null) {
                addTableRow(table, "Мероприятий с активными чатами", formatNumber(eventsWithChats) + " с чатами");
            }
        }

        document.add(table);
    }

    private void addCompletionRates(Document document, Map<String, Object> stats) {
        Paragraph header = createParagraph("ЭФФЕКТИВНОСТЬ ВЫПОЛНЕНИЯ", 16, true, TextAlignment.LEFT);
        header.setMarginBottom(15);
        document.add(header);

        Map<String, Object> completion = (Map<String, Object>) stats.get("completionRates");

        Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        table.addHeaderCell(createHeaderCell("Тип задач"));
        table.addHeaderCell(createHeaderCell("Уровень выполнения"));

        if (completion != null) {
            addTableRow(table, "Задачи мероприятий", formatNumber(completion.get("tasksCompleted")) + "% выполнено");
            addTableRow(table, "Элементы чек-листов", formatNumber(completion.get("checklistItemsCompleted")) + "% выполнено");

            Object overallCompletion = completion.get("overallCompletion");
            if (overallCompletion != null) {
                addTableRow(table, "Общая эффективность", formatNumber(overallCompletion) + "% выполнено");
            }
        }

        document.add(table);
    }

    private void addPopularCategories(Document document, Map<String, Object> stats) {
        Paragraph header = createParagraph("ПОПУЛЯРНЫЕ КАТЕГОРИИ", 16, true, TextAlignment.LEFT);
        header.setMarginBottom(15);
        document.add(header);

        Map<String, Object> categories = (Map<String, Object>) stats.get("popularCategories");

        if (categories != null) {
            List<Map<String, Object>> categoryList = (List<Map<String, Object>>) categories.get("categories");

            if (categoryList != null && !categoryList.isEmpty()) {
                Table table = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
                table.setWidth(UnitValue.createPercentValue(100));

                table.addHeaderCell(createHeaderCell("Категория"));
                table.addHeaderCell(createHeaderCell("Количество мероприятий"));

                for (Map<String, Object> category : categoryList) {
                    table.addCell(createCell(getSafeString(category.get("name"))));
                    table.addCell(createCell(formatNumber(category.get("eventCount"))));
                }

                document.add(table);
            } else {
                document.add(createParagraph("Нет данных по категориям", 12, false, TextAlignment.LEFT).setItalic());
            }
        } else {
            document.add(createParagraph("Нет данных по категориям", 12, false, TextAlignment.LEFT).setItalic());
        }
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\n\n"));
        Paragraph footer = createParagraph("Отчет сгенерирован автоматически системой Events Platform", 8, false, TextAlignment.CENTER);
        footer.setItalic();
        document.add(footer);
    }

    // Вспомогательные методы
    private Paragraph createParagraph(String text, int fontSize, boolean bold, TextAlignment alignment) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setFontSize(fontSize);
        paragraph.setTextAlignment(alignment);
        if (bold) {
            paragraph.setBold();
        }
        return paragraph;
    }

    private Cell createHeaderCell(String text) {
        Cell cell = new Cell();
        cell.add(createParagraph(text, 12, true, TextAlignment.CENTER));
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell.setPadding(5);
        return cell;
    }

    private Cell createCell(String text) {
        Cell cell = new Cell();
        cell.add(createParagraph(text, 10, false, TextAlignment.LEFT));
        cell.setPadding(5);
        return cell;
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(createCell(label));
        table.addCell(createCell(value));
    }

    private String getSafeString(Object obj) {
        if (obj == null) {
            return "0";
        }
        return obj.toString();
    }

    private String formatNumber(Object obj) {
        if (obj == null) {
            return "0";
        }
        return obj.toString();
    }

    private byte[] createErrorPdf(String errorMessage) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            if (russianFont != null) {
                document.setFont(russianFont);
            }

            Paragraph errorTitle = createParagraph("ОШИБКА ГЕНЕРАЦИИ ОТЧЕТА", 16, true, TextAlignment.CENTER);
            errorTitle.setMarginBottom(20);
            document.add(errorTitle);

            Paragraph errorMsg = createParagraph("При генерации отчета произошла ошибка:", 12, false, TextAlignment.LEFT);
            errorMsg.setMarginBottom(10);
            document.add(errorMsg);

            Paragraph errorDetails = createParagraph(errorMessage, 10, false, TextAlignment.LEFT);
            errorDetails.setItalic();
            errorDetails.setMarginBottom(20);
            document.add(errorDetails);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error creating error PDF", e);
            return new byte[0];
        }
    }
}