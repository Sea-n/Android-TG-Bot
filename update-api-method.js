// URL: https://core.telegram.org/bots/api
// Command: pbpaste | jq . > app/src/main/assets/api-methods.json

(function () {
  let out = {};

	let anchors = document.getElementsByClassName("anchor");

	for (k=0; k<anchors.length; k++) {
		let h4 = anchors[k].parentNode;
		let methodName = h4.innerText;
		if (!methodName.match(/^[a-z]/) || methodName.match(/ /)) continue;

		let description = h4.nextElementSibling.innerText;
		var params = {};

		var paramsTable = false;
		if (h4.nextElementSibling.nextElementSibling.tagName == "TABLE") {
			paramsTable = h4.nextElementSibling.nextElementSibling.children[1].children;
		} else if (h4.nextElementSibling.nextElementSibling.nextElementSibling.tagName == "TABLE") {
			paramsTable = h4.nextElementSibling.nextElementSibling.nextElementSibling.children[1].children;
		}

		if (paramsTable) {
			for (i=0; i<paramsTable.length; i++) {
				let name = paramsTable[i].children[0].innerText;
				let type = paramsTable[i].children[1].innerText;
				let required = paramsTable[i].children[2].innerText == "Yes";
				let description = paramsTable[i].children[3].innerText;
				params[name] = {type, required, description};
			}

			if (methodName == "sendMediaGroup")
				for (i=0; i<10; i++)
					params['file' + i] = {
						type: 'InputFile',
						description: 'Pass “attach://file' + i + '” to upload a new'
					};
		}

		out[methodName] = {description, params}
	}

	console.log(out);
	prompt("result", JSON.stringify(out));
})()
