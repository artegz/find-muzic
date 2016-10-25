package ru.asm

import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.elasticsearch.node.Node
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import ru.asm.actions.DispatchResult
/**
 * User: artem.smirnov
 * Date: 10.06.2016
 * Time: 12:03
 */
class ConsoleModeApp {

    public static final String HELP = """Commands:
    help - show this help
    exit - exit program
    countall - total number of records in repo
    find <mainCategory> <subCategory> <folders> <title> <page> - find torrent by name and category
    findr <mainCategory> <subCategory> <folders> <title> <page> - find torrent by name and category
    csvdb <file> <playlist> - import songs from csv to db
    dbselect <command>

    db import <file> <playlist> [comment]
    db songs [playlist] [artist] [comment]
    db artists [playlist] [comment]
                    """

    public static final String HELP_2 = """
es countall
es scan <playlist> <page> <page size>
es find <mainCategory> <subCategory> <folders> <title> <page>
es findr <mainCategory> <subCategory> <folders> <title> <page>
db import <file> <playlist> [comment]
db list-artists <playlist>
db list-songs <playlist> [artist]
db cleanup <playlist>
db sql <sql>
help
exit
"""
    ApplicationContext applicationContext

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        final ConsoleModeApp app = new ConsoleModeApp()
        app.init()

        try {
            boolean closed = false
            while (!closed) {
                String line = scanner.nextLine()

                DispatchResult dispatchResult = app.proceedCommand(line)

                switch (dispatchResult) {
                    case DispatchResult.EXIT:
                        closed = true
                        break
                    case DispatchResult.SUCCESSFUL:
                        break
                }
            }
        } finally {
            app.destroy()
        }
    }

    public DispatchResult proceedCommand(String str) {
        def command = prepareCommand(str)
        def dispatchResult = dispatchCommand(command)
        dispatchResult
    }

    public DispatchResult dispatchCommand(UserCommand command) {
        DispatchResult dispatchResult

        try {
            def commandType = command.command.trim().toLowerCase()

            switch (commandType) {
                case "exit":
                    dispatchResult = DispatchResult.EXIT
                    break
                case "help":
                    println HELP_2
                    dispatchResult = DispatchResult.SUCCESSFUL
                    break
                case "es":
                    def dbCommand = CliUtils.command(command.params)
                    def dbArgs = CliUtils.args(command.params)

                    def torrentsDatabaseService = applicationContext.getBean(TorrentsDatabaseService.class)

                    switch (dbCommand) {
                        case "scan":
                            def playlist = CliUtils.arg(dbArgs, 0)
                            def page = CliUtils.arg(dbArgs, 1)
                            def pageSize = CliUtils.arg(dbArgs, 2)

                            break
                        case "countall":
                            torrentsDatabaseService.printTotalCount()
                            break
                        case "findr":
                        case "find":
                            def mainCatQueue = CliUtils.arg(dbArgs, 0)
                            def subCatQueue = CliUtils.arg(dbArgs, 1)
                            def folderQueue = CliUtils.arg(dbArgs, 2)
                            def titleQueue = CliUtils.arg(dbArgs, 3)
                            def page = CliUtils.arg(dbArgs, 4)

                            if (dbCommand.equals("findr")) {
                                torrentsDatabaseService.findAndPrint(true, mainCatQueue, subCatQueue, folderQueue, titleQueue, page != null ? Integer.valueOf(page) : 0)
                            } else {
                                torrentsDatabaseService.findAndPrint(false, mainCatQueue, subCatQueue, folderQueue, titleQueue, page != null ? Integer.valueOf(page) : 0)
                            }
                            break
                        default:
                            println "unknown command"
                    }

                    dispatchResult = DispatchResult.SUCCESSFUL

                    break
                case "db":
                    def dbCommand = CliUtils.command(command.params)
                    def dbArgs = CliUtils.args(command.params)

                    def dataStorageService = applicationContext.getBean(DataStorageService.class)

                    switch (dbCommand) {
                        case "import":
                            def fileName = CliUtils.arg(dbArgs, 0)
                            def playlist = CliUtils.arg(dbArgs, 1)
                            def comment = CliUtils.arg(dbArgs, 2)

                            def file = new File(fileName)
                            if (file.exists()) {
                                dataStorageService.importPlaylist(file, playlist, comment)
                            } else {
                                println "file ${fileName} does not exist"
                            }

                            break
                        case "list-artists":
                            def playlist = CliUtils.arg(dbArgs, 0)
                            dataStorageService.listArtists(playlist)
                            break
                        case "list-songs":
                            def playlist = CliUtils.arg(dbArgs, 0)
                            def artist = CliUtils.arg(dbArgs, 1)
                            dataStorageService.listSongs(artist, playlist)
                            break
                        case "cleanup":
                            def playlist = CliUtils.arg(dbArgs, 0)
                            dataStorageService.cleanupSongs(playlist)
                            break
                        case "sql":
                            def sql = CliUtils.arg(dbArgs, 0)
                            dataStorageService.executeSql(sql)
                            break
                        default:
                            println "unknown command"
                    }

                    dispatchResult = DispatchResult.SUCCESSFUL
                    break
                default:
                    println "unknown command"
                    dispatchResult = DispatchResult.ERROR
            }
        } catch (Throwable e) {
            e.printStackTrace()
            dispatchResult = DispatchResult.ERROR;
        }

        return dispatchResult
    }


    private static UserCommand prepareCommand(String str) {
        def parts = CliUtils.split(str)
        def command = CliUtils.command(parts)

        if (parts.size() > 1) {
            return new UserCommand(command, CliUtils.args(parts))
        } else {
            return new UserCommand(command)
        }
    }

    public void init() {
        this.applicationContext = new AnnotationConfigApplicationContext(DataStorageConfig.class, ElasticsearchConfig.class)
        def elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class)

        // await at least for yellow status
        def response = elasticsearchOperations.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get()
        if (response.getStatus() != ClusterHealthStatus.YELLOW) {
            throw new AssertionError();
        }
    }

    public void destroy() {
        def node = applicationContext.getBean(Node.class)
        node.close();
    }

}
