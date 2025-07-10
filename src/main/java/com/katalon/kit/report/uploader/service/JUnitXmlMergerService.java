package com.katalon.kit.report.uploader.service;

import com.katalon.kit.report.uploader.helper.FileHelper;
import com.katalon.kit.report.uploader.helper.LogHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;

@Service
public class JUnitXmlMergerService {
    private static final Logger log = LogHelper.getLogger();

    @Autowired
    private FileHelper fileHelper;

    /**
     * Merges all JUnit XML files found in the specified directory and its subdirectories
     * into a single XML file at the same level as the input directory.
     * 
     * @param directoryPath The path to the directory containing JUnit XML files
     * @return Path to the merged XML file, or null if no files were found or error occurred
     */
    public Path mergeJUnitXmlFiles(String directoryPath) {
        try {
            log.info("Starting JUnit XML merge for directory: {}", directoryPath);
            
            // Find all XML files in the directory
            List<Path> xmlFiles = fileHelper.scanFiles(directoryPath, ".*\\.xml$");
            
            if (xmlFiles.isEmpty()) {
                log.warn("No XML files found in directory: {}", directoryPath);
                return null;
            }
            
            log.info("Found {} XML files to merge", xmlFiles.size());
            
            // Create merged document
            Document mergedDoc = createMergedDocument(xmlFiles);
            
            if (mergedDoc == null) {
                log.error("Failed to create merged document");
                return null;
            }
            
            // Save merged document
            Path outputPath = saveMergedDocument(mergedDoc, directoryPath);
            
            log.info("Successfully merged {} XML files into: {}", xmlFiles.size(), outputPath);
            return outputPath;
            
        } catch (Exception e) {
            log.error("Error merging JUnit XML files", e);
            return null;
        }
    }

    /**
     * Creates a merged document from multiple JUnit XML files
     */
    private Document createMergedDocument(List<Path> xmlFiles) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document mergedDoc = builder.newDocument();
            
            // Create root testsuites element (JUnit standard)
            Element rootElement = mergedDoc.createElement("testsuites");
            mergedDoc.appendChild(rootElement);
            
            // Map to track unique test cases by name (classname + name) with timestamp info
            Map<String, TestCaseInfo> uniqueTestCases = new LinkedHashMap<>();
            
            // Initialize aggregated statistics
            int totalTests = 0;
            int totalFailures = 0;
            int totalErrors = 0;
            int totalSkipped = 0;
            double totalTime = 0.0;
            String mergedName = "MergedTestSuite";
            
            // Track suite names to determine if they're all the same
            Set<String> suiteNames = new HashSet<>();
            
            // Process each XML file
            for (Path xmlFile : xmlFiles) {
                try {
                    Document doc = builder.parse(new FileInputStream(xmlFile.toFile()));
                    Element rootDocElement = doc.getDocumentElement();
                    
                    // Handle both testsuites and testsuite root elements
                    NodeList testsuitesList;
                    if ("testsuites".equals(rootDocElement.getTagName())) {
                        // Root is testsuites, get all testsuite children
                        testsuitesList = rootDocElement.getElementsByTagName("testsuite");
                        // Collect suite name from testsuites element
                        String suiteName = rootDocElement.getAttribute("name");
                        if (!suiteName.isEmpty()) {
                            suiteNames.add(suiteName);
                        }
                    } else if ("testsuite".equals(rootDocElement.getTagName())) {
                        // Root is testsuite, use it directly
                        testsuitesList = doc.getElementsByTagName("testsuite");
                        // Collect suite name from testsuite element
                        String suiteName = rootDocElement.getAttribute("name");
                        if (!suiteName.isEmpty()) {
                            suiteNames.add(suiteName);
                        }
                    } else {
                        log.warn("Skipping file {} - not a valid JUnit XML structure", xmlFile);
                        continue;
                    }
                    
                    // Process each testsuite element
                    for (int suiteIndex = 0; suiteIndex < testsuitesList.getLength(); suiteIndex++) {
                        Element testsuiteElement = (Element) testsuitesList.item(suiteIndex);
                        
                        // Collect suite name from testsuite element
                        String suiteName = testsuiteElement.getAttribute("name");
                        if (!suiteName.isEmpty()) {
                            suiteNames.add(suiteName);
                        }
                        
                        // Get timestamp for precedence determination
                        String timestampStr = testsuiteElement.getAttribute("timestamp");
                        Instant timestamp = parseTimestamp(timestampStr);
                        
                        // Copy properties (only from first file to avoid duplicates)
                        if (uniqueTestCases.isEmpty()) {
                            NodeList properties = testsuiteElement.getElementsByTagName("properties");
                            if (properties.getLength() > 0) {
                                Element propertiesElement = (Element) properties.item(0);
                                Element mergedProperties = (Element) mergedDoc.importNode(propertiesElement, true);
                                rootElement.appendChild(mergedProperties);
                            }
                        }
                        
                        // Process all testcase elements
                        NodeList testcases = testsuiteElement.getElementsByTagName("testcase");
                        for (int i = 0; i < testcases.getLength(); i++) {
                            Element testcase = (Element) testcases.item(i);
                            String classname = testcase.getAttribute("classname");
                            String name = testcase.getAttribute("name");
                            String testKey = classname + "#" + name;
                            
                            // Check if this test case already exists
                            TestCaseInfo existingTestCaseInfo = uniqueTestCases.get(testKey);
                            if (existingTestCaseInfo != null) {
                                // Handle duplicate test case (rerun scenario)
                                Element mergedTestcase = handleDuplicateTestCase(mergedDoc, existingTestCaseInfo, testcase, timestamp);
                                uniqueTestCases.put(testKey, new TestCaseInfo(mergedTestcase, timestamp));
                                log.debug("Updated duplicate test case: {}#{} with timestamp: {}", classname, name, timestamp);
                            } else {
                                // New test case
                                Element mergedTestcase = (Element) mergedDoc.importNode(testcase, true);
                                uniqueTestCases.put(testKey, new TestCaseInfo(mergedTestcase, timestamp));
                                log.debug("Added new test case: {}#{} with timestamp: {}", classname, name, timestamp);
                            }
                        }
                    }
                    
                    log.debug("Processed file: {}", xmlFile);
                    
                } catch (Exception e) {
                    log.error("Error processing file: {}", xmlFile, e);
                }
            }
            
            // Determine the suite name - use original if all are the same, otherwise use merged name
            if (suiteNames.size() == 1) {
                mergedName = suiteNames.iterator().next();
                log.debug("All reports have same suite name: {}", mergedName);
            } else {
                log.debug("Reports have different suite names: {}, using merged name", suiteNames);
            }
            
            // Create a single testsuite element for the merged results
            Element mergedTestsuite = mergedDoc.createElement("testsuite");
            rootElement.appendChild(mergedTestsuite);
            
            // Add all unique test cases to the merged testsuite
            for (TestCaseInfo testCaseInfo : uniqueTestCases.values()) {
                mergedTestsuite.appendChild(testCaseInfo.element);
            }
            
            // Calculate final statistics from unique test cases
            for (TestCaseInfo testCaseInfo : uniqueTestCases.values()) {
                Element testcase = testCaseInfo.element;
                totalTests++;
                totalTime += getDoubleAttribute(testcase, "time", 0.0);
                
                // Check for failures - look for both failure elements and status attribute
                NodeList failures = testcase.getElementsByTagName("failure");
                String status = testcase.getAttribute("status");
                if (failures.getLength() > 0 || "FAILED".equals(status)) {
                    totalFailures++;
                }
                
                // Check for errors
                NodeList errors = testcase.getElementsByTagName("error");
                if (errors.getLength() > 0) {
                    totalErrors++;
                }
                
                // Check for skipped
                NodeList skipped = testcase.getElementsByTagName("skipped");
                if (skipped.getLength() > 0) {
                    totalSkipped++;
                }
            }
            
            // Set aggregated attributes on the testsuite element
            mergedTestsuite.setAttribute("tests", String.valueOf(totalTests));
            mergedTestsuite.setAttribute("failures", String.valueOf(totalFailures));
            mergedTestsuite.setAttribute("errors", String.valueOf(totalErrors));
            mergedTestsuite.setAttribute("skipped", String.valueOf(totalSkipped));
            mergedTestsuite.setAttribute("time", String.valueOf(totalTime));
            mergedTestsuite.setAttribute("name", mergedName);
            
            // Also set attributes on the testsuites root element
            rootElement.setAttribute("name", mergedName);
            rootElement.setAttribute("time", String.valueOf(totalTime));
            rootElement.setAttribute("tests", String.valueOf(totalTests));
            rootElement.setAttribute("failures", String.valueOf(totalFailures));
            rootElement.setAttribute("errors", String.valueOf(totalErrors));
            
            return mergedDoc;
            
        } catch (Exception e) {
            log.error("Error creating merged document", e);
            return null;
        }
    }

    /**
     * Handles duplicate test cases from reruns by keeping the most recent result based on timestamp
     * Priority: Latest timestamp > Pass > Error > Failure > Skipped
     */
    private Element handleDuplicateTestCase(Document mergedDoc, TestCaseInfo existingTestCaseInfo, Element newTestcase, Instant newTimestamp) {
        Element existingTestcase = existingTestCaseInfo.element;
        Instant existingTimestamp = existingTestCaseInfo.timestamp;
        
        // First priority: Use the latest timestamp (most recent run)
        if (newTimestamp != null && existingTimestamp != null) {
            if (newTimestamp.isAfter(existingTimestamp)) {
                log.debug("Preferring newer test case based on timestamp: {} > {}", newTimestamp, existingTimestamp);
                return (Element) mergedDoc.importNode(newTestcase, true);
            } else if (existingTimestamp.isAfter(newTimestamp)) {
                log.debug("Keeping existing test case based on timestamp: {} > {}", existingTimestamp, newTimestamp);
                return existingTestcase;
            }
        }
        
        // If timestamps are equal or null, fall back to status-based priority
        // Get status of both test cases
        boolean existingHasFailure = existingTestcase.getElementsByTagName("failure").getLength() > 0;
        boolean existingHasError = existingTestcase.getElementsByTagName("error").getLength() > 0;
        boolean existingHasSkipped = existingTestcase.getElementsByTagName("skipped").getLength() > 0;
        boolean existingIsPass = !existingHasFailure && !existingHasError && !existingHasSkipped;
        
        boolean newHasFailure = newTestcase.getElementsByTagName("failure").getLength() > 0;
        boolean newHasError = newTestcase.getElementsByTagName("error").getLength() > 0;
        boolean newHasSkipped = newTestcase.getElementsByTagName("skipped").getLength() > 0;
        boolean newIsPass = !newHasFailure && !newHasError && !newHasSkipped;
        
        // Priority: Pass > Error > Failure > Skipped
        if (newIsPass && !existingIsPass) {
            // New test case passed, existing didn't - prefer new (success)
            log.debug("Preferring passed test case over failed one");
            return (Element) mergedDoc.importNode(newTestcase, true);
        } else if (existingIsPass && !newIsPass) {
            // Existing test case passed, new didn't - keep existing (success)
            log.debug("Keeping existing passed test case");
            return existingTestcase;
        } else if (newHasError && !existingHasError) {
            // New test case has error, existing doesn't - prefer new
            log.debug("Preferring new test case with error");
            return (Element) mergedDoc.importNode(newTestcase, true);
        } else if (existingHasError && !newHasError) {
            // Existing test case has error, new doesn't - keep existing
            log.debug("Keeping existing test case with error");
            return existingTestcase;
        } else if (newHasFailure && !existingHasFailure) {
            // New test case has failure, existing doesn't - prefer new
            log.debug("Preferring new test case with failure");
            return (Element) mergedDoc.importNode(newTestcase, true);
        } else if (existingHasFailure && !newHasFailure) {
            // Existing test case has failure, new doesn't - keep existing
            log.debug("Keeping existing test case with failure");
            return existingTestcase;
        } else if (newHasSkipped && !existingHasSkipped) {
            // New test case is skipped, existing isn't - prefer new
            log.debug("Preferring new skipped test case");
            return (Element) mergedDoc.importNode(newTestcase, true);
        } else if (existingHasSkipped && !newHasSkipped) {
            // Existing test case is skipped, new isn't - keep existing
            log.debug("Keeping existing skipped test case");
            return existingTestcase;
        } else {
            // Both have same status, prefer the one with more details or newer
            int existingChildCount = existingTestcase.getChildNodes().getLength();
            int newChildCount = newTestcase.getChildNodes().getLength();
            
            if (newChildCount > existingChildCount) {
                // New test case has more details (longer error message, etc.)
                log.debug("Preferring new test case with more details");
                return (Element) mergedDoc.importNode(newTestcase, true);
            } else {
                // Prefer newer one if same detail level
                log.debug("Preferring newer test case");
                return (Element) mergedDoc.importNode(newTestcase, true);
            }
        }
    }

    /**
     * Parses timestamp string to Instant
     */
    private Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(timestampStr);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse timestamp: {}", timestampStr);
            return null;
        }
    }

    /**
     * Helper class to store test case element with its timestamp
     */
    private static class TestCaseInfo {
        final Element element;
        final Instant timestamp;
        
        TestCaseInfo(Element element, Instant timestamp) {
            this.element = element;
            this.timestamp = timestamp;
        }
    }

    /**
     * Saves the merged document to a file
     */
    private Path saveMergedDocument(Document mergedDoc, String directoryPath) {
        try {
            Path outputPath = Path.of(directoryPath).resolve("merged-junit-report.xml");
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            DOMSource source = new DOMSource(mergedDoc);
            StreamResult result = new StreamResult(new FileOutputStream(outputPath.toFile()));
            transformer.transform(source, result);
            
            return outputPath;
            
        } catch (Exception e) {
            log.error("Error saving merged document", e);
            return null;
        }
    }

    /**
     * Helper method to get integer attribute from element
     */
    private int getIntAttribute(Element element, String attributeName, int defaultValue) {
        try {
            String value = element.getAttribute(attributeName);
            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Helper method to get double attribute from element
     */
    private double getDoubleAttribute(Element element, String attributeName, double defaultValue) {
        try {
            String value = element.getAttribute(attributeName);
            return value.isEmpty() ? defaultValue : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
} 