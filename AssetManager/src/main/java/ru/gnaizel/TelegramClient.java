package ru.gnaizel;

import ca.denisab85.tdlight.Client;
import ca.denisab85.tdlight.ClientManager;
import ca.denisab85.tdlight.Init;
import ca.denisab85.tdlight.exception.CantLoadLibrary;
import ca.denisab85.tdlight.jni.TdApi;

public class TelegramClient {
    private final int API_ID = Integer.parseInt(System.getenv("TELEGRAM_API_ID"));
    private final String API_HASH = System.getenv("TELEGRAM_API_HASH");
    private final String DOWNLOAD_DIR = System.getenv("DOWNLOAD_DIR");
    private Client client;
    private AuthorizationHandler authorizationHandler;

    public void start() throws CantLoadLibrary, InterruptedException {
        Init.start();
        client = ClientManager.create();
        authorizationHandler = new AuthorizationHandler(client);

        TdApi.TdlibParameters params = new TdApi.TdlibParameters();
        params.apiId = API_ID;
        params.apiHash = API_HASH;
        params.databaseDirectory = "tdlib_db";
        params.filesDirectory = "tdlib_files";

        client.initialize(authorizationHandler, null, null);
        client.send(new TdApi.SetTdlibParameters(params), null);

        // Подписываемся на обновления
        client.addUpdateHandler(TdApi.UpdateAuthorizationState.CONSTRUCTOR, authorizationHandler);
        client.addUpdateHandler(TdApi.UpdateNewMessage.CONSTRUCTOR, new UpdateHandler(client, DOWNLOAD_DIR));
    }
}
