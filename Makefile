all: publish-base generate-docs

publish-base:
	docker build --no-cache -t "codacy-cppcheck-base:latest" -f Dockerfile . --build-arg toolVersion="$(shell cat .cppcheckVersion | tr -d '\n')"

generate-docs:
	docker run -i "codacy-cppcheck-base:latest" --errorlist > .tmp_errorlist
	sbt "doc-generator/run .tmp_errorlist"
	rm -rf .tmp_errorlist
