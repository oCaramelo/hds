package tecnico.configs;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import tecnico.exceptions.ErrorMessage;
import tecnico.exceptions.HDSSException;

public class ProcessConfigBuilder {

    public ProcessConfig[] fromFile(String path) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(input, ProcessConfig[].class);
        } catch (FileNotFoundException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_NOT_FOUND);
        } catch (JsonSyntaxException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_FORMAT);
        }
    }

    public NodeConfig[] fromNodesFile(String path) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(input, NodeConfig[].class);
        } catch (FileNotFoundException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_NOT_FOUND);
        } catch (JsonSyntaxException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_FORMAT);
        }
    }

    public ClientConfig[] fromClientFile(String path) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(input, ClientConfig[].class);
        } catch (FileNotFoundException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_NOT_FOUND);
        } catch (JsonSyntaxException e) {
            throw new HDSSException(ErrorMessage.CONFIG_FILE_FORMAT);
        }
    }
}