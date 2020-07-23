all: publish-docker generate-docs

publish-docker:
	docker build --no-cache -t "codacy-cppcheck:latest" . --build-arg toolVersion="$(shell cat .cppcheckVersion | tr -d '\n')"

generate-docs:
	docker run -i --entrypoint cppcheck "codacy-cppcheck:latest" --errorlist > .tmp_errorlist
	sbt "doc-generator/run .tmp_errorlist"
	rm -rf .tmp_errorlist
