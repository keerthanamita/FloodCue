<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') sendJson(405, ['ok'=>false,'error'=>'method_not_allowed']);
try {
    $scannerUid=authenticateFirebaseUid(); $body=readJsonBody(); $token=trim((string)($body['token']??''));
    if ($token==='' || strlen($token)>128) sendJson(400,['ok'=>false,'error'=>'invalid_qr_token']);
    global $config; $pdo=getDatabase(); $allowed=$scannerUid===(string)($config['qr_admin_uid']??'');
    if (!$allowed) { $s=$pdo->prepare('SELECT 1 FROM emergency_qr_authorized_users WHERE firebase_uid=:uid AND active=1 LIMIT 1'); $s->execute(['uid'=>$scannerUid]); $allowed=(bool)$s->fetchColumn(); }
    if (!$allowed) sendJson(403,['ok'=>false,'error'=>'scanner_not_authorized']);
    $s=$pdo->prepare('SELECT owner_firebase_uid FROM emergency_qr_tokens WHERE token_hash=:hash AND revoked_at IS NULL AND expires_at>NOW() LIMIT 1');
    $s->execute(['hash'=>hash('sha256',$token)]); $owner=$s->fetchColumn();
    if (!is_string($owner)||$owner==='') sendJson(410,['ok'=>false,'error'=>'qr_expired_or_revoked']);
    $columns=array_merge(['firebase_uid'],profileFields()); $s=$pdo->prepare('SELECT '.implode(', ',$columns).' FROM users WHERE firebase_uid=:uid LIMIT 1'); $s->execute(['uid'=>$owner]); $profile=$s->fetch();
    if (!$profile) sendJson(404,['ok'=>false,'error'=>'profile_not_found']); unset($profile['firebase_uid']); sendJson(200,['ok'=>true,'profile'=>$profile]);
} catch (Throwable $e) { error_log('FloodCue scan_emergency_qr error: '.$e->getMessage()); sendJson(500,['ok'=>false,'error'=>'server_error']); }
