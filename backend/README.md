Kitchen Backend Server
==================

    cd kitchenfantasy/backend

    bin/sbt gen-idea
    ## this will generate files for intellij -- if you want them

    bin/sbt eclipse
    ## this will generate files for eclipse -- if you need them.

    bin/sbt "kitchen-server/run rest/src/main/resources/server.config"
    ## this will run the server

    bin/sbt kitchen-server/assembly

    ## this will build the deployable jar, along with a config, 
    ## it's all you need

    bin/sbt "kitchen-server/run rest/src/main/resources/server.config --init-data"

    ## this will import an initial list of products. 
