<?php
/**
 * FloodCue IMD proxy
 * ---------------------------------------------------------------------------
 * IMD (api.imd.gov.in) issues an API key that is bound to one server address,
 * so the key cannot live inside the Android app. Upload this single file to
 * your own hosting (for example HelioHost), put your IMD key in it below, and
 * point the app at it: Flood Alerts -> IMD data source.
 *
 *   https://yourname.heliohost.org/imd_proxy.php
 *   https://yourname.heliohost.org/imd_proxy.php?endpoint=districtnowcast
 *
 * The app itself works without this file: NDMA SACHET alerts are fetched
 * directly. This file only adds IMD district rainfall warnings.
 * ---------------------------------------------------------------------------
 */

declare(strict_types=1);

// >>> Put your IMD API key here. Never commit a real key to a public repo. <<<
$IMD_KEY = 'PASTE_YOUR_IMD_API_KEY_HERE';

// Only needed if the IMD portal gives you a JWT as well as the key.
$IMD_JWT = '';

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

$allowed = array(
    'districtwarning' => 'districtwarning',
    'districtnowcast' => 'districtnowcast',
);

$endpoint = isset($_GET['endpoint']) ? (string) $_GET['endpoint'] : 'districtwarning';
if (!isset($allowed[$endpoint])) {
    http_response_code(400);
    echo json_encode(array('error' => 'unknown endpoint'));
    exit;
}

// District id is optional; without it IMD returns the full district list.
$id = '';
if (isset($_GET['id']) && preg_match('/^[0-9]{1,6}$/', (string) $_GET['id'])) {
    $id = (string) $_GET['id'];
}

$url = 'https://api.imd.gov.in/api/v1/' . $allowed[$endpoint]
     . ($id !== '' ? '?id=' . $id : '');

$headers = array(
    'X-API-KEY: ' . $IMD_KEY,
    'Accept: application/json',
);
if ($IMD_JWT !== '') {
    $headers[] = 'Authorization: Bearer ' . $IMD_JWT;
}

$curl = curl_init();
curl_setopt_array($curl, array(
    CURLOPT_URL            => $url,
    CURLOPT_HTTPHEADER     => $headers,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CONNECTTIMEOUT => 10,
    CURLOPT_TIMEOUT        => 25,
    CURLOPT_FOLLOWLOCATION => true,
));

$body = curl_exec($curl);
$status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
$error = curl_error($curl);
curl_close($curl);

if ($body === false || $body === '') {
    http_response_code(502);
    echo json_encode(array('error' => 'IMD did not answer', 'detail' => $error));
    exit;
}

if ($status !== 200) {
    http_response_code(502);
    echo json_encode(array('error' => 'IMD returned status ' . $status));
    exit;
}

echo $body;
