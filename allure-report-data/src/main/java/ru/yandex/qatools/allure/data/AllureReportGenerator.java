package ru.yandex.qatools.allure.data;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import ru.yandex.qatools.allure.data.converters.TestCaseConverter;
import ru.yandex.qatools.allure.data.io.Reader;
import ru.yandex.qatools.allure.data.io.ReportWriter;
import ru.yandex.qatools.allure.data.plugins.PluginManager;
import ru.yandex.qatools.allure.model.TestCaseResult;
import ru.yandex.qatools.commons.model.Environment;

import java.io.File;

import static ru.yandex.qatools.allure.data.utils.AllureReportUtils.createDirectory;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 12.02.15
 */
public class AllureReportGenerator {

    public static final String DATA_DIRECTORY_NAME = "data";

    @Inject
    private Reader<TestCaseResult> testCaseReader;

    @Inject
    private Reader<Environment> environmentReader;

    @Inject
    private Reader<AttachmentInfo> attachmentReader;

    @Inject
    private TestCaseConverter converter;

    @Inject
    private PluginManager pluginManager;

    public AllureReportGenerator(File... inputDirectories) {
        this(AllureReportGenerator.class.getClassLoader(), inputDirectories);
    }

    public AllureReportGenerator(ClassLoader pluginClassLoader, File... inputDirectories) {
        this(new AllureGuiceModule(pluginClassLoader, inputDirectories));
    }

    /**
     * For testing only
     */
    AllureReportGenerator(AbstractModule module) {
        Guice.createInjector(module).injectMembers(this);
    }

    public void generate(File outputDirectory) {
        File reportDataDirectory = createDirectory(outputDirectory, DATA_DIRECTORY_NAME);

        ReportWriter writer = new ReportWriter(reportDataDirectory);
        generate(writer);
    }

    public void generate(ReportWriter writer) {
        for (TestCaseResult result : testCaseReader) {
            pluginManager.prepare(result);
            
            AllureTestCase testCase = converter.convert(result);
            pluginManager.prepare(testCase);
            pluginManager.process(testCase);
            writer.write(testCase);
        }

        pluginManager.writePluginData(AllureTestCase.class, writer);

        for (Environment environment : environmentReader) {
            pluginManager.prepare(environment);
            pluginManager.process(environment);
        }

        pluginManager.writePluginData(Environment.class, writer);

        for (AttachmentInfo attachment : attachmentReader) {
            pluginManager.prepare(attachment);
            writer.write(attachment);
        }

        writer.close();
    }
}
