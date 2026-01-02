host="http://localhost:9200"
count=10000
index=tmdb
tmdb_dump_path=../data/movies_${count}.json
user_query=basketball with cartoon aliens

#====================================================================
help:   ## Show this help.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


#====================================================================

up: 	## Run elasticsearch in docker container
	docker-compose up -d

down:	## Stop elasticsearch docker container
	docker-compose down

crawl:	## Download fresh data via TMDB API
	time ./tmdb_crawler.py -c ${count} -f ${tmdb_dump_path}

index:	## Index downloaded data
	time ./tmdb_indexer.py -h ${host} -f ${tmdb_dump_path} -i ${index}

query:	## Perform test search query
	./query.py -h ${host} -i ${index} -q "${user_query}" #--explain --analyze

