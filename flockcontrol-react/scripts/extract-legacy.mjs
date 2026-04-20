import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');
const sourceHtmlPath = path.resolve(projectRoot, '..', 'FlockControl-Complete.html');

if (!fs.existsSync(sourceHtmlPath)) {
  console.error(`Source HTML not found at: ${sourceHtmlPath}`);
  process.exit(1);
}

const html = fs.readFileSync(sourceHtmlPath, 'utf8');

const styleMatch = html.match(/<style>([\s\S]*?)<\/style>/i);
if (!styleMatch) {
  console.error('Could not find <style>...</style> block in source HTML.');
  process.exit(1);
}

const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>/i);
if (!scriptMatch) {
  console.error('Could not find <script>...</script> block in source HTML.');
  process.exit(1);
}

const bodyMatch = html.match(/<body>([\s\S]*?)<script>/i);
if (!bodyMatch) {
  console.error('Could not find <body>...<script> boundary in source HTML.');
  process.exit(1);
}

const outDir = path.resolve(projectRoot, 'src', 'legacy');
fs.mkdirSync(outDir, { recursive: true });

const outCssPath = path.resolve(outDir, 'legacy.css');
const outBodyPath = path.resolve(outDir, 'legacy-body.html');
const outScriptPath = path.resolve(outDir, 'legacy-script.js');

fs.writeFileSync(outCssPath, styleMatch[1].trim() + '\n', 'utf8');
fs.writeFileSync(outBodyPath, bodyMatch[1].trim() + '\n', 'utf8');
fs.writeFileSync(outScriptPath, scriptMatch[1].trim() + '\n', 'utf8');

console.log('Extracted legacy assets:');
console.log(`- ${path.relative(projectRoot, outCssPath)}`);
console.log(`- ${path.relative(projectRoot, outBodyPath)}`);
console.log(`- ${path.relative(projectRoot, outScriptPath)}`);

