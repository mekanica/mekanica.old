
.PHONY: build

ifndef SKIPUPLOAD
SKIPUPLOAD=true # Skip uploads unless explicitly enabled
endif

ifndef RELEASE
RELEASE=alpha
endif

all:
	./gradlew build

dev:
	./gradlew setupDecompWorkspace idea

clean:	
	rm -rf build .gradle .idea run out *.iml *.ipr *.iws
	git clean -dfx

rel:
	$(MAKE) build RELEASE=rel

rel.upload:
	$(MAKE) build RELEASE=rel SKIPUPLOAD=false

beta:
	$(MAKE) build RELEASE=beta

beta.upload:
	$(MAKE) build RELEASE=beta SKIPUPLOAD=false

alpha:
	$(MAKE) build RELEASE=alpha

alpha.upload:
	$(MAKE) build RELEASE=alpha SKIPUPLOAD=false

build:
	mkdir -p build.docker
	docker build -t mekanica .
	docker run -a stdin -a stdout -e CF_API_TOKEN -e RELEASE=$(RELEASE) \
		--mount type=bind,src=`pwd`/build.docker,dst=/build.docker \
		-w /mekanica -it mekanica make docker.build

docker.build:
	python3 release.py \
		-project=315844:mekanica: \
		-skipupload=$(SKIPUPLOAD) \
		-mcvsn 1.12.2 -rel $(RELEASE)
	cp build/libs/* /build.docker